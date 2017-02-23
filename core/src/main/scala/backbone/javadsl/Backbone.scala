package backbone.javadsl

import java.util.concurrent.{CompletionStage, Future => JFuture}
import java.util.function.{Function => JFunction1}

import akka.Done
import akka.actor.ActorSystem
import backbone.format.Format
import backbone.scaladsl.Backbone.{ProcessingResult, ConsumerSettings => SConsumerSettings}
import backbone.scaladsl.{Backbone => SBackbone}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient

import scala.collection.JavaConverters._
import scala.compat.java8.{FunctionConverters, FutureConverters, OptionConverters}

class Backbone(val sqs: AmazonSQSAsyncClient, val sns: AmazonSNSAsyncClient) {

  val asScala = new SBackbone()(sqs, sns)

  def consume[T](settings: ConsumerSettings,
                 format: Format[T],
                 actorSystem: ActorSystem,
                 f: JFunction1[T, ProcessingResult]): JFuture[Done] = {


    val asScalaSettings = new SConsumerSettings(settings.events.asScala.toList,
                                                settings.topics.asScala.toList,
                                                settings.queue,
                                                settings.parallelism,
                                                OptionConverters.toScala(settings.consumeWithin))

    val asScalaFunction = FunctionConverters.asScalaFromFunction(f)
    FutureConverters.toJava(asScala.consume(asScalaSettings)(asScalaFunction)(actorSystem, format)).toCompletableFuture
  }

  def consumeAsync[T](settings: SConsumerSettings,
                      format: Format[T],
                      actorSystem: ActorSystem,
                      f: JFunction1[T, CompletionStage[ProcessingResult]]): JFuture[Done] = {

    val asScalaFunction: (T) => CompletionStage[ProcessingResult] = FunctionConverters.asScalaFromFunction(f)
    val asScalaFuture                                             = asScalaFunction.andThen(a => FutureConverters.toScala(a))
    FutureConverters.toJava(asScala.consumeAsync[T](settings)(asScalaFuture)(actorSystem, format)).toCompletableFuture

  }
}

object Backbone {
  def create(sqs: AmazonSQSAsyncClient, sns: AmazonSNSAsyncClient): Backbone = {
    new Backbone(sqs, sns)
  }
}
