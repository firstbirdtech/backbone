package backbone.javadsl

import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.{Function => JFunction1}

import akka.Done
import akka.actor.ActorSystem
import backbone.{MessageReader, scaladsl, _}
import backbone.consumer.ConsumerSettings
import backbone.json.JsonReader
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient

import scala.compat.java8.{FunctionConverters, FutureConverters}

/** Subscribing to certain kinds of events from various SNS topics and consume them via a Amazon SQS queue.
 *
 * @param sqs AmazonSQSASyncClient
 * @param sns AmazonSNSAsyncClient
 */
class Backbone(val sqs: AmazonSQSAsyncClient, val sns: AmazonSNSAsyncClient, val system: ActorSystem) {

  val asScala = new scaladsl.Backbone()(sqs, sns, system)

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f      function which processes objects of type T and returns a ProcessingResult
   * @param actorSystem implicit actor system
   * @param format     Format[T] typeclass instance descirbing how to decode SQS Message to T
   * @tparam T type of envents to consume
   * @return a java future completing when the stream quits
   */
  def consume[T](settings: ConsumerSettings,
                 format: MessageReader[T],
                 f: JFunction1[T, ProcessingResult]): CompletableFuture[Done] = {

    val asScalaFunction = FunctionConverters.asScalaFromFunction(f)
    FutureConverters.toJava(asScala.consume(settings)(asScalaFunction)(format)).toCompletableFuture
  }

  /** Consume elements of type T until an optional condition in ConsumerSettings is met.
   *
   * Creates a queue with the name provided in settings if it does not already exist. Subscribes
   * the queue to all provided topics and modifies the AWS Policy to allow sending messages to
   * the queue from the topics.
   *
   * @param settings ConsumerSettings configuring Backbone
   * @param f      function which processes objects of type T and returns a Future[ProcessingResult]
   * @param actorSystem implicit actor system
   * @param format     Format[T] typeclass instance describing how to decode SQS Message to T
   * @tparam T type of events to consume
   * @return a java future completing when the stream quits
   */
  def consumeAsync[T](settings: ConsumerSettings,
                      format: MessageReader[T],
                      f: JFunction1[T, CompletionStage[ProcessingResult]]): CompletableFuture[Done] = {

    val asScalaFunction: (T) => CompletionStage[ProcessingResult] = FunctionConverters.asScalaFromFunction(f)
    val asScalaFuture                                             = asScalaFunction.andThen(a => FutureConverters.toScala(a))
    FutureConverters.toJava(asScala.consumeAsync[T](settings)(asScalaFuture)(format)).toCompletableFuture

  }
}

object Backbone {
  def create(sqs: AmazonSQSAsyncClient, sns: AmazonSNSAsyncClient, system: ActorSystem): Backbone = {
    new Backbone(sqs, sns, system)
  }
}
