package backbone.scaladsl

import akka.Done
import akka.actor.ActorSystem
import backbone.aws.{AmazonSnsOps, AmazonSqsOps}
import backbone.consumer.{Consumer, Limitation}
import backbone.format.Format
import backbone.scaladsl.Backbone.{ConsumerSettings, ProcessingResult, QueueInformation}
import com.amazonaws.auth.policy.actions.SQSActions
import com.amazonaws.auth.policy.conditions.ConditionFactory
import com.amazonaws.auth.policy.{Policy, Principal, Resource, Statement}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient

import scala.concurrent.{ExecutionContext, Future}

object Backbone {

  case class QueueInformation(url: String, arn: String)

  sealed trait ProcessingResult
  case object Rejected extends ProcessingResult
  case object Consumed extends ProcessingResult

  case class SnsEnvelope(subject: String, message: String)

  case class Envelope[P](deleteHandle: String, subject: String, payload: P)

  object ConsumerSettings {
    def apply(events: List[String], topics: List[String], queue: String, cosumeWithin: Limitation): ConsumerSettings =
      apply(events, topics, queue, 1, Some(cosumeWithin))

    def apply(events: List[String],
              topics: List[String],
              queue: String,
              parallelism: Int,
              cosumeWithin: Limitation): ConsumerSettings =
      apply(events, topics, queue, parallelism, Some(cosumeWithin))

  }

  /**
   *
   * @param events        a list of events to listen to
   * @param topics        a list of topics to subscribe to
   * @param queue         the name of a queue to consumer from
   * @param parallelism   number of concurrent messages in process
   * @param consumeWithin optional limitation when backbone should stop consuming
   */
  case class ConsumerSettings(
      events: List[String],
      topics: List[String],
      queue: String,
      parallelism: Int = 1,
      consumeWithin: Option[Limitation] = None
  )

  def apply()(implicit sqs: AmazonSQSAsyncClient, sns: AmazonSNSAsyncClient): Backbone = new Backbone()

}

/** Subscribing to certain kinds of events from various SNS topics and consume them via a Amazon SQS queue.
 *
 * @param sqs implicit aws sqs async client
 * @param sns implicit aws sns async client
 */
class Backbone(implicit val sqs: AmazonSQSAsyncClient, val sns: AmazonSNSAsyncClient)
    extends AmazonSqsOps
    with AmazonSnsOps {

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f      function which processes objects of type T and returns a ProcessingResult
   * @param system implicit actor system
   * @param fo     Format[T] typeclass instance descirbing how to decode SQS Message to T
   * @tparam T type of envents to consume
   * @return a future completing when the stream quits
   */
  def consume[T](settings: ConsumerSettings)(f: T => ProcessingResult)(implicit system: ActorSystem,
                                                                       fo: Format[T]): Future[Done] = {
    consumeAsync[T](settings)(f.andThen(Future.successful))
  }

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f      function which processes objects of type T and returns a Future[ProcessingResult]
   * @param system implicit actor system
   * @param fo     Format[T] typeclass instance descirbing how to decode SQS Message to T
   * @tparam T type of envents to consume
   * @return a future completing when the stream quits
   */
  def consumeAsync[T](settings: ConsumerSettings)(f: T => Future[ProcessingResult])(implicit system: ActorSystem,
                                                                                    fo: Format[T]): Future[Done] = {
    implicit val ec = system.dispatcher

    for {
      queue <- createQueue(settings.queue)
      _     <- subscribe(queue, settings.topics)
      s = Consumer.Settings(queue.url, settings.events, settings.parallelism, settings.consumeWithin)
      r <- new Consumer(s).consumeAsync(f)
    } yield r

  }

  private[this] def subscribe(queue: QueueInformation, topics: List[String])(
      implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- updatePolicy(queue, topics)
      _ <- Future.sequence(topics.map(t => subscribe(queue, t)))
    } yield ()
  }

  private[this] def updatePolicy(queue: QueueInformation, topics: List[String])(
      implicit ec: ExecutionContext): Future[Unit] = {
    topics match {
      case Nil => Future.successful(())
      case ts  => savePolicy(queue.url, createPolicy(queue.arn, ts))
    }
  }

  private[this] def createPolicy(queueArn: String, topicsArns: Seq[String]): Policy = {
    val statements = topicsArns.map { arn =>
      new Statement(Statement.Effect.Allow)
        .withId("topic-subscription-" + arn)
        .withPrincipals(Principal.AllUsers)
        .withActions(SQSActions.SendMessage)
        .withResources(new Resource(queueArn))
        .withConditions(ConditionFactory.newSourceArnCondition(arn))
    }

    new Policy().withStatements(statements: _*)
  }

}
