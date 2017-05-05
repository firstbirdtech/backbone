package backbone.scaladsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import backbone.aws.{AmazonSnsOps, AmazonSqsOps}
import backbone.consumer.{Consumer, ConsumerSettings}
import backbone.json.JsonReader
import backbone.publisher.{Publisher, PublisherSettings}
import backbone.scaladsl.Backbone.QueueInformation
import backbone.{MessageReader, MessageWriter, ProcessingResult}
import com.amazonaws.auth.policy.actions.SQSActions
import com.amazonaws.auth.policy.conditions.ConditionFactory
import com.amazonaws.auth.policy.{Policy, Principal, Resource, Statement}
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sqs.AmazonSQSAsync

import scala.concurrent.{ExecutionContext, Future}

object Backbone {

  case class QueueInformation(url: String, arn: String)

  def apply()(implicit sqs: AmazonSQSAsync, sns: AmazonSNSAsync, system: ActorSystem): Backbone =
    new Backbone()

}

/** Subscribing to certain kinds of events from various SNS topics and consume them via a Amazon SQS queue,
 * and publish messages to an Amazon SNS topic.
 *
 * @param sqs    implicit aws sqs async client
 * @param sns    implicit aws sns async client
 * @param system implicit actor system
 */
class Backbone(implicit val sqs: AmazonSQSAsync, val sns: AmazonSNSAsync, system: ActorSystem)
    extends AmazonSqsOps
    with AmazonSnsOps {

  private[this] val config                          = system.settings.config
  private[this] val jsonReaderClass                 = Class.forName(config.getString("backbone.json.reader"))
  private[this] implicit val jsonReader: JsonReader = jsonReaderClass.newInstance().asInstanceOf[JsonReader]

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f        function which processes objects of type T and returns a ProcessingResult
   * @param fo       Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T type of events to consume
   * @return a future completing when the stream quits
   */
  def consume[T](settings: ConsumerSettings)(f: T => ProcessingResult)(implicit fo: MessageReader[T]): Future[Done] = {
    consumeAsync[T](settings)(f.andThen(Future.successful))
  }

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f        function which processes objects of type T and returns a Future[ProcessingResult]
   * @param fo       Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T type of events to consume
   * @return a future completing when the stream quits
   */
  def consumeAsync[T](settings: ConsumerSettings)(f: T => Future[ProcessingResult])(
      implicit fo: MessageReader[T]): Future[Done] = {
    implicit val ec = system.dispatcher

    for {
      queue <- createQueue(settings.queue)
      _     <- subscribe(queue, settings.topics)
      s = Consumer.Settings(queue.url, settings.events, settings.parallelism, settings.consumeWithin)
      r <- new Consumer(s).consumeAsync(f)
    } yield r
  }

  /**
   * Publish a single element of type T to an AWS SNS topic.
   *
   * @param message the message to publish
   * @param settings PublisherSettings configuring Backbone
   * @param mw typeclass instance describing how to write the message to a String
   * @tparam T type of message to publish
   * @return a future completing when the stream quits
   */
  def publishAsync[T](message: T, settings: PublisherSettings)(implicit mw: MessageWriter[T]): Future[Done] = {
    new Publisher(Publisher.Settings(settings.topicArn)).publishAsync(message :: Nil)
  }

  /**
   * Publish a list of elements of type T to an AWS SNS topic.
   *
   * @param messages the messages to publish
   * @param settings PublisherSettings configuring Backbone
   * @param mw typeclass instance describing how to write a single message to a String
   * @tparam T type of messages to publish
   * @return a future completing when the stream quits
   */
  def publishAsync[T](messages: List[T], settings: PublisherSettings)(implicit mw: MessageWriter[T]): Future[Done] = {
    new Publisher(Publisher.Settings(settings.topicArn)).publishAsync(messages)
  }

  /**
   * An actor reference that publishes received elements of type T to an AWS SNS topic.
   *
   * @param settings PublisherSettings configuring Backbone
   * @param bufferSize size of the buffer
   * @param overflowStrategy strategy to use if the buffer is full
   * @param mw typeclass instance describing how to write a single message to a String
   * @tparam T type of messages to publish
   * @return an ActorRef that publishes received messages
   */
  def actorPublisher[T](
      settings: PublisherSettings,
      bufferSize: Int = Int.MaxValue,
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead)(implicit mw: MessageWriter[T]): ActorRef = {
    new Publisher(Publisher.Settings(settings.topicArn)).actorPublisher(bufferSize, overflowStrategy)
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
