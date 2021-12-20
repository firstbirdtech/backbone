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

package backbone.publisher.javadsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.javadsl.Sink
import backbone.MessageWriter
import backbone.publisher.{Settings, scaladsl}
import software.amazon.awssdk.services.sns.SnsAsyncClient

import java.util.concurrent.CompletableFuture
import scala.annotation.varargs
import scala.compat.java8.FutureConverters

object Publisher {

  /**
   * Creates a Publisher instance
   *
   * @param system
   *   implicit ActorSystem
   * @param sns
   *   implicit SnsAsyncClient
   * @return
   *   a Publisher instance
   */
  def create(system: ActorSystem, sns: SnsAsyncClient): Publisher = new Publisher()(system, sns)

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

  val asScala: scaladsl.Publisher = scaladsl.Publisher()(system, sns)

  /**
   * Publish messages of type A to an AWS SNS topic.
   *
   * @param settings
   *   publisher settings
   * @param mw
   *   typeclass instance describing how to write the message to a String
   * @param messages
   *   the messages to publish
   * @tparam A
   *   type of message to publish
   * @return
   *   a future completing when the stream quits
   */
  @varargs
  def publishAsync[A](settings: Settings, mw: MessageWriter[A], messages: A*): CompletableFuture[Done] = {
    val scalaFuture = asScala.publishAsync[A](settings)(messages: _*)(mw)
    FutureConverters.toJava(scalaFuture).toCompletableFuture
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
  def sink[A](settings: Settings, mw: MessageWriter[A]): Sink[A, CompletableFuture[Done]] = {
    asScala
      .sink(settings)(mw)
      .mapMaterializedValue(FutureConverters.toJava(_).toCompletableFuture)
      .asJava
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
  def actor[A](
      settings: Settings,
      bufferSize: Int,
      overflowStrategy: OverflowStrategy,
      mw: MessageWriter[A]): ActorRef = {
    asScala.actor(settings)(bufferSize, overflowStrategy)(mw)
  }

}
