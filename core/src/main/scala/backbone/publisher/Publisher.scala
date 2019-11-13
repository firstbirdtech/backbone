package backbone.publisher

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Keep, RestartFlow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy, Supervision}
import backbone.MessageWriter
import backbone.publisher.Publisher.Settings
import com.amazonaws.services.sns.AmazonSNSAsync
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

object Publisher {
  case class Settings(topicArn: String)
}

/**
 * INTERNAL API
 */
private[backbone] class Publisher(settings: Settings)(implicit system: ActorSystem, sns: AmazonSNSAsync) {

  private val logger = LoggerFactory.getLogger(getClass)

  private[this] implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(_ => Supervision.Resume)
  )

  def publishAsync[T](messages: List[T])(implicit mw: MessageWriter[T]): Future[Done] = {
    Source(messages)
      .runWith(sink)
  }

  def sink[T](implicit mw: MessageWriter[T]): Sink[T, Future[Done]] = {
    RestartFlow
      .withBackoff(1.second, 30.seconds, 0.2) { () =>
        Flow[T]
          .map(mw.write)
          .log(getClass.getName, t => s"Publishing message to SNS. $t")
          .via(SnsPublisher.flow(settings.topicArn))
          .mapError {
            case ex =>
              logger.error("Exception in publishing message to SNS.", ex)
              ex
          }
      }
      .toMat(Sink.ignore)(Keep.right)
  }

  def actor[T](bufferSize: Int, overflowStrategy: OverflowStrategy)(implicit mw: MessageWriter[T]): ActorRef = {
    Source
      .actorRef(bufferSize, overflowStrategy)
      .to(sink)
      .run()
  }

}
