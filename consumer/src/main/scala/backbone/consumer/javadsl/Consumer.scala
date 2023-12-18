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

package backbone.consumer.javadsl

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import backbone.consumer.{JsonReader, Settings, scaladsl}
import backbone.{MessageReader, ProcessingResult}
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.{Function => JFunction1}
import scala.compat.java8.{FunctionConverters, FutureConverters}

object Consumer {

  /**
   * Creates a Publisher instance
   *
   * @param jsonReader
   *   a JsonReader implementation
   * @param system
   *   an ActorSystem
   * @param sqs
   *   an AmazonSQSAsyncClient
   * @return
   *   a Publisher instance
   */
  def create(jsonReader: JsonReader, system: ActorSystem, sqs: SqsAsyncClient): Consumer = {
    new Consumer(jsonReader, system, sqs)
  }

}

/**
 * Consumes messages from an SQS queue
 *
 * @param jsonReader
 *   a JsonReader implementation
 * @param system
 *   an ActorSystem
 * @param sqs
 *   an AmazonSQSAsyncClient
 */
class Consumer(jsonReader: JsonReader, system: ActorSystem, sqs: SqsAsyncClient) {

  val asScala: scaladsl.Consumer = scaladsl.Consumer(jsonReader)(system, sqs)

  /**
   * Consume messages of type A until an optional condition in settings is met.
   *
   * After successfully processing messages of type A they are removed from the queue.
   *
   * @param settings
   *   consumer settings
   * @param mr
   *   MessageReader describing how to decode SQS Message to A
   * @param f
   *   function which processes objects of type A and returns a ProcessingResult
   * @tparam A
   *   type of message to consume
   * @return
   *   a future completing when the stream quits
   */
  def consume[A](
      settings: Settings,
      mr: MessageReader[A],
      f: JFunction1[A, ProcessingResult]): CompletableFuture[Done] = {
    val scalaFunction = FunctionConverters.asScalaFromFunction(f)
    val scalaFuture   = asScala.consume(settings)(scalaFunction)(mr)
    FutureConverters.toJava(scalaFuture).toCompletableFuture
  }

  /**
   * Consume messages of type A until an optional condition in settings is met.
   *
   * After successfully processing messages of type A they are removed from the queue.
   *
   * @param settings
   *   consumer settings
   * @param mr
   *   MessageReader describing how to decode SQS Message to A
   * @param f
   *   function which processes objects of type A and returns a CompletionStage[ProcessingResult]
   * @tparam A
   *   type of message to consume
   * @return
   *   a future completing when the stream quits
   */
  def consumeAsync[A](
      settings: Settings,
      mr: MessageReader[A],
      f: JFunction1[A, CompletionStage[ProcessingResult]]): CompletableFuture[Done] = {
    val scalaFunction       = FunctionConverters.asScalaFromFunction(f)
    val scalaFutureFunction = scalaFunction.andThen(a => FutureConverters.toScala(a))
    val scalaFuture         = asScala.consumeAsync(settings)(scalaFutureFunction)(mr)
    FutureConverters.toJava(scalaFuture).toCompletableFuture
  }

}
