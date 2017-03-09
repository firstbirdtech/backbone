package backbone.javadsl.consumer

import java.util.concurrent.{CompletionStage, Future}
import java.util.function.{Function => JFunction}

import akka.Done
import akka.actor.ActorSystem
import backbone.consumer.Consumer.{Settings => SSettings}
import backbone.consumer.{Consumer => SConsumer}
import backbone.format.Format
import backbone.scaladsl.Backbone.ProcessingResult
import com.amazonaws.services.sqs.AmazonSQSAsyncClient

import scala.collection.JavaConverters._
import scala.compat.java8.{FunctionConverters, FutureConverters, OptionConverters}

class Consumer(settings:Settings,system:ActorSystem,sqs:AmazonSQSAsyncClient) {
  val asScalaSettings = SSettings(
    settings.queueUrl,
    settings.event.asScala.toList,
    settings.parallelism,
    OptionConverters.toScala(settings.limitation)
  )
  val asScala = new SConsumer(asScalaSettings)(system,sqs)

  def consumeAsync[T](fo:Format[T],f: JFunction[T, CompletionStage[ProcessingResult]]): Future[Done] = {
    val asScalaFunction: (T) => CompletionStage[ProcessingResult] = FunctionConverters.asScalaFromFunction(f)
    val asScalaFuture                                             = asScalaFunction.andThen(a => FutureConverters.toScala(a))
    FutureConverters.toJava(asScala.consumeAsync[T](asScalaFuture)(fo)).toCompletableFuture
  }

}

object Consumer {

}
