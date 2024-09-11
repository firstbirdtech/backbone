/*
 * Copyright (c) 2024 Backbone contributors
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

package backbone.javadsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.javadsl.Sink
import backbone.consumer.ConsumerSettings
import backbone.publisher.PublisherSettings
import backbone.{MessageReader, scaladsl, _}
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.{Function => JFunction1}
import java.util.{List => JList}
import scala.compat.java8.{FunctionConverters, FutureConverters}
import scala.jdk.CollectionConverters._

/**
 * Subscribing to certain kinds of events from various SNS topics and consume them via a Amazon SQS queue, and publish
 * messages to an Amazon SNS topic.
 *
 * @param sqs
 *   aws sqs async client
 * @param sns
 *   aws sns async client
 * @param system
 *   actor system
 */
class Backbone(val sqs: SqsAsyncClient, val sns: SnsAsyncClient, val system: ActorSystem) {

  val asScala = new scaladsl.Backbone()(sqs, sns, system)

  /**
   * Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes the queue to all
   * provided topics and modifies the AWS Policy to allow sending messages to the queue from the topics.
   *
   * @param settings
   *   ConsumerSettings configuring Backbone
   * @param f
   *   function which processes objects of type T and returns a ProcessingResult
   * @param format
   *   Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T
   *   type of events to consume
   * @return
   *   a java future completing when the stream quits
   */
  def consume[T](
      settings: ConsumerSettings,
      format: MessageReader[T],
      f: JFunction1[T, ProcessingResult]): CompletableFuture[Done] = {

    val asScalaFunction = FunctionConverters.asScalaFromFunction(f)
    FutureConverters.toJava(asScala.consume(settings)(asScalaFunction)(format)).toCompletableFuture
  }

  /**
   * Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes the queue to all
   * provided topics and modifies the AWS Policy to allow sending messages to the queue from the topics.
   *
   * @param settings
   *   ConsumerSettings configuring Backbone
   * @param f
   *   function which processes objects of type T and returns a Future[ProcessingResult]
   * @param format
   *   Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T
   *   type of events to consume
   * @return
   *   a java future completing when the stream quits
   */
  def consumeAsync[T](
      settings: ConsumerSettings,
      format: MessageReader[T],
      f: JFunction1[T, CompletionStage[ProcessingResult]]): CompletableFuture[Done] = {

    val asScalaFunction = FunctionConverters.asScalaFromFunction(f)
    val asScalaFuture   = asScalaFunction.andThen(a => FutureConverters.toScala(a))

    FutureConverters.toJava(asScala.consumeAsync[T](settings)(asScalaFuture)(format)).toCompletableFuture
  }

  /**
   * Publish a single element of type T to an AWS SNS topic.
   *
   * @param message
   *   the message to publish
   * @param settings
   *   PublisherSettings configuring Backbone
   * @param mw
   *   typeclass instance describing how to write the message to a String
   * @tparam T
   *   type of message to publish
   * @return
   *   a future completing when the stream quits
   */
  def publishAsync[T](message: T, settings: PublisherSettings, mw: MessageWriter[T]): CompletableFuture[Done] = {
    FutureConverters.toJava(asScala.publishAsync(message, settings)(mw)).toCompletableFuture
  }

  /**
   * Publish a list of elements of type T to an AWS SNS topic.
   *
   * @param msgs
   *   the messages to publish
   * @param settings
   *   PublisherSettings configuring Backbone
   * @param mw
   *   typeclass instance describing how to write a single message to a String
   * @tparam T
   *   type of messages to publish
   * @return
   *   a future completing when the stream quits
   */
  def publishAsync[T](msgs: JList[T], settings: PublisherSettings, mw: MessageWriter[T]): CompletableFuture[Done] = {
    val asScalaList = msgs.asScala.toList
    FutureConverters.toJava(asScala.publishAsync(asScalaList, settings)(mw)).toCompletableFuture
  }

  /**
   * An actor reference that publishes received elements of type T to an AWS SNS topic.
   *
   * @param settings
   *   PublisherSettings configuring Backbone
   * @param bufferSize
   *   size of the buffer
   * @param overflowStrategy
   *   strategy to use if the buffer is full
   * @param mw
   *   typeclass instance describing how to write a single message to a String
   * @tparam T
   *   type of messages to publish
   * @return
   *   an ActorRef that publishes received messages
   */
  def actorPublisher[T](
      settings: PublisherSettings,
      bufferSize: Int,
      overflowStrategy: OverflowStrategy,
      mw: MessageWriter[T]): ActorRef = {
    asScala.actorPublisher(settings, bufferSize, overflowStrategy)(mw)
  }

  /**
   * Returns a sink that publishes received messages of type T to an AWS SNS topic.
   *
   * @param settings
   *   PublisherSettings configuring Backbone
   * @param mw
   *   typeclass instance describing how to write a single message to a String
   * @tparam T
   *   type of messages to publish
   * @return
   *   a Sink that publishes received messages
   */
  def publisherSink[T](settings: PublisherSettings, mw: MessageWriter[T]): Sink[T, CompletableFuture[Done]] = {
    asScala
      .publisherSink(settings)(mw)
      .mapMaterializedValue(FutureConverters.toJava(_).toCompletableFuture)
      .asJava
  }

}

object Backbone {
  def create(sqs: SqsAsyncClient, sns: SnsAsyncClient, system: ActorSystem): Backbone = {
    new Backbone(sqs, sns, system)
  }
}
