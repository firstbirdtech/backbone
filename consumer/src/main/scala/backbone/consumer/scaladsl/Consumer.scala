/*
 * Copyright (c) 2021 Backbone contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package backbone.consumer.scaladsl

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.alpakka.sqs.scaladsl.{SqsAckFlow, SqsSource}
import akka.stream.alpakka.sqs.{MessageAction, MessageAttributeName, SqsSourceSettings}
import akka.stream.scaladsl.{Flow, Keep, RestartSource, Sink}
import akka.stream.{RestartSettings, Supervision}
import backbone.consumer.{JsonReader, MessageHeaders, Settings}
import backbone._
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Left, Right, Success}
object Consumer {

  /**
   * Creates a Consumer instance by reading a JsonReader implementation from config
   *
   * @param system
   *   implicit ActorSystem
   * @param sqs
   *   implicit SqsAsyncClient
   * @return
   *   a Consumer instance
   */
  def apply()(implicit system: ActorSystem, sqs: SqsAsyncClient): Consumer = {
    val jsonReaderClass = Class.forName(system.settings.config.getString("backbone.json.reader"))
    val jsonReader      = jsonReaderClass.getDeclaredConstructor().newInstance().asInstanceOf[JsonReader]
    apply(jsonReader)
  }

  /**
   * Creates a Consumer instance with a given JsonReader implementation
   *
   * @param jsonReader
   *   a JsonReader implementation
   * @param system
   *   implicit ActorSystem
   * @param sqs
   *   implicit SqsAsyncClient
   * @return
   *   a Consumer instance
   */
  def apply(jsonReader: JsonReader)(implicit system: ActorSystem, sqs: SqsAsyncClient): Consumer = {
    new Consumer(jsonReader)
  }

}

/**
 * Consumes messages from an SQS queue
 *
 * @param jsonReader
 *   a JsonReader implementation
 * @param system
 *   implicit ActorSystem
 * @param sqs
 *   implicit SqsAsyncClient
 */
class Consumer(jsonReader: JsonReader)(implicit system: ActorSystem, sqs: SqsAsyncClient) {

  private[this] val logger                       = LoggerFactory.getLogger(getClass)
  private[this] implicit val log: LoggingAdapter = Logging(system, getClass)

  private[this] implicit val ec: ExecutionContextExecutor = system.dispatcher
  private[this] val restartingDecider: Supervision.Decider = { t =>
    logger.error("Error on Consumer stream.", t)
    Supervision.Restart
  }

  /**
   * Consume messages of type A until an optional condition in settings is met.
   *
   * After successfully processing messages of type A they are removed from the queue.
   *
   * @param settings
   *   consumer settings
   * @param f
   *   function which processes objects of type A and returns a ProcessingResult
   * @param mr
   *   implicit MessageReader describing how to decode SQS Message to A
   * @tparam A
   *   type of message to consume
   * @return
   *   a future completing when the stream quits
   */
  def consume[A](settings: Settings)(f: A => ProcessingResult)(implicit mr: MessageReader[A]): Future[Done] = {
    consumeAsync[A](settings)(f.andThen(Future.successful))
  }

  /**
   * Consume messages of type A and available headers until an optional condition in settings is met.
   *
   * After successfully processing messages of type A they are removed from the queue.
   *
   * @param settings
   *   consumer settings
   * @param f
   *   function which processes objects of type A and MessageHeaders and returns a Future[ProcessingResult]
   * @param mr
   *   implicit MessageReader describing how to decode SQS Message to A
   * @tparam A
   *   type of message to consume
   * @return
   *   a future completing when the stream quits
   */
  def consumeWithHeadersAsync[A](settings: Settings)(f: (A, MessageHeaders) => Future[ProcessingResult])(implicit
      mr: MessageReader[A]): Future[Done] = {
    logger.info(s"Starting to consume messages off SQS queue. settings=$settings")

    val baseSqsSourceSettings = SqsSourceSettings.Defaults
      .withWaitTimeSeconds(settings.receiveSettings.waitTimeSeconds)
      .withMaxBatchSize(settings.receiveSettings.maxBatchSize)
      .withMaxBufferSize(settings.receiveSettings.maxBufferSize)
      .withParallelRequests(settings.receiveSettings.parallelRequests)
      .withAttributes(settings.receiveSettings.attributeNames)
      .withMessageAttributes(settings.receiveSettings.messageAttributeNames)
      .withCloseOnEmptyReceive(settings.receiveSettings.closeOnEmptyReceive)
      .withMessageAttributes(MessageAttributeName("All") :: Nil)

    val sqsSourceSettings = settings.receiveSettings.visibilityTimeout match {
      case Some(visibilityTimeout) => baseSqsSourceSettings.withVisibilityTimeout(visibilityTimeout)
      case None                    => baseSqsSourceSettings
    }

    RestartSource
      .withBackoff(RestartSettings(3.second, 30.seconds, 0.2))(() => SqsSource(settings.queueUrl, sqsSourceSettings))
      .via(settings.limitation.map(_.limit[Message]).getOrElse(Flow[Message]))
      .mapAsync(settings.parallelism) { implicit message =>
        logger.debug(s"Received message from SQS. message=$message ")

        val headers = MessageHeaders(message.messageAttributes.asScala.collect {
          case (k, v) if v.dataType == "String" => k -> v.stringValue()
        }.toMap)
        parseMessage[A](message) match {
          case Left(a) =>
            Future.successful(a)
          case Right(t) =>
            val future = f(t, headers).map(resultToAction)
            future.onComplete {
              case Success(_) => logger.debug(s"Successfully processed message. messageId=${message.messageId}")
              case Failure(t) => logger.warn(s"Failed processing message. messageId=${message.messageId}", t)
            }
            future
        }
      }
      .withAttributes(supervisionStrategy(restartingDecider))
      .runWith(ack(settings))
  }

  /**
   * Consume messages of type A until an optional condition in settings is met.
   *
   * After successfully processing messages of type A they are removed from the queue.
   *
   * @param settings
   *   consumer settings
   * @param f
   *   function which processes objects of type A and returns a Future[ProcessingResult]
   * @param mr
   *   implicit MessageReader describing how to decode SQS Message to A
   * @tparam A
   *   type of message to consume
   * @return
   *   a future completing when the stream quits
   */
  def consumeAsync[A](settings: Settings)(f: A => Future[ProcessingResult])(implicit
      mr: MessageReader[A]): Future[Done] = {
    logger.info(s"Starting to consume messages off SQS queue. settings=$settings")

    consumeWithHeadersAsync(settings)((a: A, _) => f(a))
  }

  private[this] def resultToAction(r: ProcessingResult)(implicit message: Message): MessageAction = {
    r match {
      case Rejected => MessageAction.Ignore(message)
      case Consumed => MessageAction.Delete(message)
    }
  }

  private[this] def parseMessage[A](message: Message)(implicit mr: MessageReader[A]): Either[MessageAction, A] = {
    for {
      sns <- jsonReader.readSnsEnvelope(message.body).toRight(MessageAction.Ignore(message))
      t <- mr.read(sns.message) match {
        case Failure(t) =>
          logger.error(s"Unable to read message. message=${message.body}", t)
          Left[MessageAction, A](MessageAction.Ignore(message))
        case Success(None) =>
          logger.info(s"MessageReader returned empty when parsing message. message=${message.body}")
          Left[MessageAction, A](MessageAction.Delete(message))
        case Success(Some(value)) =>
          Right[MessageAction, A](value)
      }
    } yield t
  }

  private[this] def ack(settings: Settings): Sink[MessageAction, Future[Done]] = {
    Flow[MessageAction]
      .log(
        "ack",
        {
          case a: MessageAction.Ignore                  => s"Keeping message on queue. id=${a.message.messageId}"
          case a: MessageAction.Delete                  => s"Removing message from queue. id=${a.message.messageId}"
          case a: MessageAction.ChangeMessageVisibility => s"Changing visibility of message. id=${a.message.messageId}"
        }
      )
      .via(SqsAckFlow(settings.queueUrl))
      .withAttributes(supervisionStrategy(restartingDecider))
      .toMat(Sink.ignore)(Keep.right)
  }
}
