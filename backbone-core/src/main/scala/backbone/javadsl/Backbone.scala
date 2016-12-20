package backbone.javadsl

import backbone.scaladsl.Backbone.{ConsumerSettings, ProcessingResult}
import java.util.function.{Function => JFunction1}

import akka.actor.ActorSystem
import backbone.format.Format
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import backbone.scaladsl.{Backbone => SBackbone}

class Backbone(val sqs: AmazonSQSAsyncClient, val sns: AmazonSNSAsyncClient) {

  val asScala = new SBackbone()(sqs,sns)

  def consume[T](settings: ConsumerSettings, format: Format[T], actorSystem: ActorSystem, f: JFunction1[T,ProcessingResult]) = {


    asScala.consume(settings)()
  }


}
