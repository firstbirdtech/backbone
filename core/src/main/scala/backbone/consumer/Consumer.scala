package backbone.consumer

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import backbone.aws.AmazonSqsOps
import backbone.consumer.Consumer.{Settings, _}
import backbone.json.JsonReader
import backbone.{MessageReader, _}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message

import scala.concurrent.Future
import scala.util.{Failure, Left, Right, Success, Try}

object Consumer {

  sealed trait MessageAction
  case class RemoveMessage(receiptHandle: String) extends MessageAction
  case object KeepMessage                         extends MessageAction

  case class Settings(
      queueUrl: String,
      events: List[String],
      parallelism: Int = 1,
      limitation: Option[Limitation] = None,
      waitTime: Int = 20,
      maxBufferSize: Int = 100,
      maxBatchSize: Int = 10
  ) {
    assert(parallelism > 0, "Parallelism must be positive")
  }

}

/** Consumes events from a queue.
 *
 * @param settings consumer settings
 * @param system implicit ActorSystem
 * @param jr a Json Reader implementation which
 * @param sqs implicit AmazonSQSAsyncClient
 */
class Consumer(settings: Settings)(implicit system: ActorSystem, val sqs: AmazonSQSAsync, jr: JsonReader)
    extends AmazonSqsOps {

  private[this] implicit val ec = system.dispatcher
  private[this] implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(_ => Supervision.resume)
  )

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

    SqsSource(settings.queueUrl, SqsSourceSettings(settings.waitTime, settings.maxBufferSize, settings.maxBatchSize))
      .via(settings.limitation.map(_.limit[Message]).getOrElse(Flow[Message]))
      .mapAsync(settings.parallelism) { implicit message =>
        parseMessage[T](message) match {
          case Left(a)  => Future.successful(a)
          case Right(t) => f(t).map(resultToAction)
        }
      }
      .runWith(ack)
  }

  private[this] def resultToAction(r: ProcessingResult)(implicit message: Message): MessageAction = r match {
    case Rejected => KeepMessage
    case Consumed => RemoveMessage(message.getReceiptHandle)
  }

  private[this] def parseMessage[T](message: Message)(implicit fo: MessageReader[T]): Either[MessageAction, T] = {
    for {
      sns <- jr.readSnsEnvelope(message.getBody).right
      _ <- {
        if (settings.events.contains(sns.subject)) {
          Right(())
        } else {
          Left(RemoveMessage(message.getReceiptHandle))
        }
      }.right
      t <- (Try(fo.read(sns.message)) match {
        case Failure(_)     => Left[MessageAction, T](KeepMessage)
        case Success(value) => Right[MessageAction, T](value)
      }).right
    } yield t
  }

  private[this] def ack: Sink[MessageAction, Future[Done]] = {
    Flow[MessageAction]
      .mapAsync(1) {
        case KeepMessage      => Future.successful(())
        case RemoveMessage(h) => deleteMessage(settings.queueUrl, h)
      }
      .toMat(Sink.ignore)((_, f) => f)
  }
}
