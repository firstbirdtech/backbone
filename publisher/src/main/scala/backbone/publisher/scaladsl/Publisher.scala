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

package backbone.publisher.scaladsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Keep, RestartFlow, Sink, Source}
import akka.stream.{OverflowStrategy, RestartSettings, Supervision}
import backbone.MessageWriter
import backbone.publisher.Settings
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sns.SnsAsyncClient

import scala.concurrent.Future
import scala.concurrent.duration._

object Publisher {

  /**
   * Create a Publisher instance
   * @param system
   *   implicit ActorSystem
   * @param sns
   *   implicit SnsAsyncClient
   * @return
   *   a Publisher instance
   */
  def apply()(implicit system: ActorSystem, sns: SnsAsyncClient): Publisher = new Publisher()

}

/**
 * Publishes messages to an SNS topic
 *
 * @param system
 *   implicit ActorSystem
 * @param sns
 *   implicit SnsAsyncClient
 */
class Publisher(implicit system: ActorSystem, sns: SnsAsyncClient) {

  private[this] val logger                       = LoggerFactory.getLogger(getClass)
  private[this] implicit val log: LoggingAdapter = Logging(system, getClass)

  /**
   * Publish messages of type A to an AWS SNS topic.
   *
   * @param settings
   *   publisher settings
   * @param messages
   *   the messages to publish
   * @param mw
   *   typeclass instance describing how to write the message to a String
   * @tparam A
   *   type of message to publish
   * @return
   *   a future completing when the stream quits
   */
  def publishAsync[A](settings: Settings)(messages: A*)(implicit mw: MessageWriter[A]): Future[Done] = {
    Source(messages.toList)
      .runWith(sink(settings))
  }

  /**
   * Sink that publishes messages of type A to an AWS SNS topic.
   *
   * @param settings
   *   publisher settings
   * @param mw
   *   typeclass instance describing how to write the message to a String
   * @tparam A
   *   type of message to publish
   * @return
   *   a future completing when the stream quits
   */
  def sink[A](settings: Settings)(implicit mw: MessageWriter[A]): Sink[A, Future[Done]] = {
    RestartFlow
      .withBackoff(RestartSettings(1.second, 30.seconds, 0.2)) { () =>
        Flow[A]
          .map(mw.write)
          .log("sink", t => s"Publishing message to SNS. $t")
          .via(SnsPublisher.flow(settings.topicArn))
          .mapError { case ex =>
            logger.error("Exception in publishing message to SNS.", ex)
            ex
          }
      }
      .withAttributes(supervisionStrategy(Supervision.resumingDecider))
      .toMat(Sink.ignore)(Keep.right)
  }

  /**
   * An actor reference that publishes messages of type A to an AWS SNS topic.
   *
   * @param settings
   *   publisher settings
   * @param bufferSize
   *   size of the buffer
   * @param overflowStrategy
   *   strategy to use if the buffer is full
   * @param mw
   *   typeclass instance describing how to write the message to a String
   * @tparam A
   *   type of message to publish
   * @return
   *   ActorRef that publishes received messages
   */
  def actor[A](settings: Settings)(bufferSize: Int, overflowStrategy: OverflowStrategy)(implicit
      mw: MessageWriter[A]): ActorRef = {
    Source
      .actorRef(
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = bufferSize,
        overflowStrategy = overflowStrategy
      )
      .to(sink(settings))
      .run()
  }

}
