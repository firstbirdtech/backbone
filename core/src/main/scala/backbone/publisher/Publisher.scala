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

package backbone.publisher

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Keep, RestartFlow, Sink, Source}
import akka.stream.{OverflowStrategy, RestartSettings, Supervision}
import backbone.MessageWriter
import backbone.publisher.Publisher.Settings
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sns.SnsAsyncClient

import scala.concurrent.Future
import scala.concurrent.duration._

object Publisher {
  case class Settings(topicArn: String)
}

/**
 * INTERNAL API
 */
private[backbone] class Publisher(settings: Settings)(implicit system: ActorSystem, sns: SnsAsyncClient) {

  private val logger = LoggerFactory.getLogger(getClass)

  def publishAsync[T](messages: List[T])(implicit mw: MessageWriter[T]): Future[Done] = {
    Source(messages)
      .runWith(sink)
  }

  def sink[T](implicit mw: MessageWriter[T]): Sink[T, Future[Done]] = {
    RestartFlow
      .withBackoff(RestartSettings(1.second, 30.seconds, 0.2)) { () =>
        Flow[T]
          .map(mw.write)
          .log(getClass.getName, t => s"Publishing message to SNS. $t")
          .via(SnsPublisher.flow(settings.topicArn))
          .mapError { case ex =>
            logger.error("Exception in publishing message to SNS.", ex)
            ex
          }
      }
      .withAttributes(supervisionStrategy(Supervision.resumingDecider))
      .toMat(Sink.ignore)(Keep.right)
  }

  def actor[T](bufferSize: Int, overflowStrategy: OverflowStrategy)(implicit mw: MessageWriter[T]): ActorRef = {
    Source
      .actorRef(
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = bufferSize,
        overflowStrategy = overflowStrategy
      )
      .to(sink)
      .run()
  }

}
