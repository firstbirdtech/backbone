package backbone.scaladsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Sink
import backbone.aws.{AmazonSnsOps, AmazonSqsOps, CreateQueueParams}
import backbone.consumer.{Consumer, ConsumerSettings}
import backbone.json.JsonReader
import backbone.publisher.{Publisher, PublisherSettings}
import backbone.scaladsl.Backbone.QueueInformation
import backbone.{MessageReader, MessageWriter, ProcessingResult}
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Backbone {

  case class QueueInformation(url: String, arn: String)

  def apply()(implicit sqs: SqsAsyncClient, sns: SnsAsyncClient, system: ActorSystem): Backbone =
    new Backbone()

}

/**
 * Subscribing to certain kinds of events from various SNS topics and consume them via a Amazon SQS queue,
 * and publish messages to an Amazon SNS topic.
 *
 * @param sqs    implicit aws sqs async client
 * @param sns    implicit aws sns async client
 * @param system implicit actor system
 */
class Backbone(implicit val sqs: SqsAsyncClient, val sns: SnsAsyncClient, system: ActorSystem)
    extends AmazonSqsOps
    with AmazonSnsOps {

  private[this] val logger          = LoggerFactory.getLogger(getClass)
  private[this] val config          = system.settings.config
  private[this] val jsonReaderClass = Class.forName(config.getString("backbone.json.reader"))
  private[this] implicit val jsonReader: JsonReader =
    jsonReaderClass.getDeclaredConstructor().newInstance().asInstanceOf[JsonReader]

  /**
   * Consume elements of type T until an optional condition in ConsumerSettings is met.
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

  /**
   * Consume elements of type T until an optional condition in ConsumerSettings is met.
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
  def consumeAsync[T](
      settings: ConsumerSettings
  )(f: T => Future[ProcessingResult])(implicit fo: MessageReader[T]): Future[Done] = {
    implicit val ec = system.dispatcher

    logger.debug(s"Preparing to consume messages. config=$settings")

    val subscription = for {
      queue <- createQueue(CreateQueueParams(settings.queue, settings.kmsKeyAlias))
      _     <- subscribe(queue, settings.topics)
    } yield queue

    subscription.onComplete {
      case Success(q) => logger.debug(s"Successfully created and subscribed queue. $q")
      case Failure(t) => logger.error(s"Subscribing to the topics failed.", t)
    }

    val result = for {
      queue <- subscription
      set = Consumer.Settings(queue.url, settings.parallelism, settings.consumeWithin, settings.receiveSettings)
      r <- new Consumer(set).consumeAsync(f)
    } yield r

    result.onComplete {
      case Success(_) => logger.info("Backbone consumer stream finished successfully.")
      case Failure(t) => logger.error("Backbone consumer stream finished with an error.", t)
    }

    result
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
      overflowStrategy: OverflowStrategy = OverflowStrategy.dropHead
  )(implicit mw: MessageWriter[T]): ActorRef = {
    new Publisher(Publisher.Settings(settings.topicArn)).actor(bufferSize, overflowStrategy)
  }

  /**
   * Returns a sink that publishes received messages of type T to an AWS SNS topic.
   *
   * @param settings PublisherSettings configuring Backbone
   * @param mw typeclass instance describing how to write a single message to a String
   * @tparam T type of messages to publish
   * @return a Sink that publishes received messages
   */
  def publisherSink[T](settings: PublisherSettings)(implicit mw: MessageWriter[T]): Sink[T, Future[Done]] = {
    new Publisher(Publisher.Settings(settings.topicArn)).sink
  }

  private[this] def subscribe(queue: QueueInformation, topics: List[String])(implicit
      ec: ExecutionContext
  ): Future[Unit] = {

    logger.info(s"Subscribing queue to topics. queueArn=${queue.arn}, topicArns=$topics")
    for {
      _ <- updatePolicy(queue, topics)
      _ <- Future.sequence(topics.map(t => subscribe(queue, t)))
    } yield ()
  }

  private[this] def updatePolicy(queue: QueueInformation, topics: List[String])(implicit
      ec: ExecutionContext
  ): Future[Unit] = {
    topics match {
      case Nil => Future.successful(())
      case ts =>
        val policy = createPolicy(queue.arn, ts)
        logger.debug(s"Saving new policy for queue. queueArn=${queue.arn}, policy=$policy")
        savePolicy(queue.url, policy)
    }
  }

  private[this] def createPolicy(queueArn: String, topicsArns: Seq[String]): String = {
    val statements = topicsArns.map { topicArn =>
      s"""
         |{
         |  "Sid": "topic-subscription-arn:aws:$topicArn",
         |  "Effect": "Allow",
         |  "Principal": {
         |    "AWS": "*"
         |  },
         |  "Action": "sqs:SendMessage",
         |  "Resource": "$queueArn",
         |  "Condition": {
         |    "ArnLike": {
         |      "aws:SourceArn": "$topicArn"
         |    }
         |  }
         |}
         |""".stripMargin
    }

    val statementsJsonString = statements.mkString(",")
    s"""{ "Version": "2012-10-17", "Statement": [$statementsJsonString] }"""
  }

}
