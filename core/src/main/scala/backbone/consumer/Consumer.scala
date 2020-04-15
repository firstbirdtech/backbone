package backbone.consumer

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.{Flow, RestartSource, Sink}
import backbone.aws.AmazonSqsOps
import backbone.consumer.Consumer.{Settings, _}
import backbone.json.JsonReader
import backbone.{MessageReader, _}
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Left, Right, Success}

object Consumer {

  sealed trait MessageAction

  case class RemoveMessage(receiptHandle: String) extends MessageAction

  case object KeepMessage extends MessageAction

  case class Settings(
      queueUrl: String,
      parallelism: Int = 1,
      limitation: Option[Limitation] = None,
      receiveSettings: SqsSourceSettings = SqsSourceSettings.Defaults
  ) {
    assert(parallelism > 0, "Parallelism must be positive")
  }

}

/** Consumes events from a queue.
 *
 * @param settings consumer settings
 * @param system   implicit ActorSystem
 * @param jr       a Json Reader implementation which
 * @param sqs      implicit AmazonSQSAsyncClient
 */
class Consumer(settings: Settings)(implicit system: ActorSystem, val sqs: SqsAsyncClient, jr: JsonReader)
    extends AmazonSqsOps {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] implicit val ec: ExecutionContextExecutor = system.dispatcher
  private[this] val restartingDecider: Supervision.Decider = { t =>
    logger.error("Error on Consumer stream.", t)
    Supervision.Restart
  }

  /** Consume elements of type T until an optional condition in settings is met.
   *
   * After successfully processing elements of type T they are removed from the queue.
   *
   * @param f  function which processes objects of type T and returns a ProcessingResult
   * @param fo Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T type of events to consume
   * @return a future completing when the stream quits
   */
  def consumeAsync[T](f: T => Future[ProcessingResult])(implicit fo: MessageReader[T]): Future[Done] = {

    logger.info(s"Starting to consume messages off SQS queue. settings=$settings")

    val baseSqsSourceSettings = SqsSourceSettings.Defaults
      .withWaitTimeSeconds(settings.receiveSettings.waitTimeSeconds)
      .withMaxBufferSize(settings.receiveSettings.maxBufferSize)
      .withMaxBatchSize(settings.receiveSettings.maxBatchSize)
      .withParallelRequests(settings.receiveSettings.parallelRequests)
      .withAttributes(settings.receiveSettings.attributeNames)
      .withMessageAttributes(settings.receiveSettings.messageAttributeNames)
      .withCloseOnEmptyReceive(settings.receiveSettings.closeOnEmptyReceive)

    val sqsSourceSettings = settings.receiveSettings.visibilityTimeout match {
      case Some(visibilityTimeout) => baseSqsSourceSettings.withVisibilityTimeout(visibilityTimeout)
      case None                    => baseSqsSourceSettings
    }

    RestartSource
      .withBackoff(3.second, 30.seconds, 0.2)(() => SqsSource(settings.queueUrl, sqsSourceSettings))
      .via(settings.limitation.map(_.limit[Message]).getOrElse(Flow[Message]))
      .mapAsync(settings.parallelism) { implicit message =>
        logger.debug(s"Received message from SQS. message=$message ")
        parseMessage[T](message) match {
          case Left(a) =>
            Future.successful(a)
          case Right(t) =>
            val future = f(t).map(resultToAction)
            future.onComplete {
              case Success(_) => logger.debug(s"Successfully processed message. messageId=${message.messageId}")
              case Failure(t) => logger.warn(s"Failed processing message. messageId=${message.messageId}", t)
            }
            future
        }
      }
      .withAttributes(supervisionStrategy(restartingDecider))
      .runWith(ack)
  }

  private[this] def resultToAction(r: ProcessingResult)(implicit message: Message): MessageAction = r match {
    case Rejected => KeepMessage
    case Consumed => RemoveMessage(message.receiptHandle)
  }

  private[this] def parseMessage[T](message: Message)(implicit fo: MessageReader[T]): Either[MessageAction, T] = {
    for {
      sns <- jr.readSnsEnvelope(message.body)
      t <- (fo.read(sns.message) match {
        case Failure(t) =>
          logger.error(s"Unable to read message. message=${message.body}", t)
          Left[MessageAction, T](KeepMessage)
        case Success(None) =>
          logger.info(s"MessageReader returned empty when parsing message. message=${message.body}")
          Left[MessageAction, T](RemoveMessage(message.receiptHandle))
        case Success(Some(value)) =>
          Right[MessageAction, T](value)
      })
    } yield t
  }

  private[this] def ack: Sink[MessageAction, Future[Done]] = {
    Flow[MessageAction]
      .mapAsync(1) {
        case KeepMessage => Future.successful(())
        case RemoveMessage(h) =>
          logger.debug(s"Removing message from queue. deleteHandle=$h")
          deleteMessage(settings.queueUrl, h)
      }
      .withAttributes(supervisionStrategy(restartingDecider))
      .toMat(Sink.ignore)((_, f) => f)
  }
}
