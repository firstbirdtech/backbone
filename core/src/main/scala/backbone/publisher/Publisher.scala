package backbone.publisher

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy, Supervision}
import backbone.MessageWriter
import backbone.publisher.Publisher.Settings
import com.amazonaws.services.sns.AmazonSNSAsync

import scala.concurrent.Future

object Publisher {
  case class Settings(topicArn: String)
}

/**
 * INTERNAL API
 */
private[backbone] class Publisher(settings: Settings)(implicit system: ActorSystem, sns: AmazonSNSAsync) {

  private[this] implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(_ => Supervision.Resume)
  )

  def publishAsync[T](messages: List[T])(implicit mw: MessageWriter[T]): Future[Done] = {
    Source(messages)
      .map[String](mw.write)
      .runWith(SnsPublisher.sink(settings.topicArn))
  }

  def actorPublisher[T](bufferSize: Int, overflowStrategy: OverflowStrategy)(implicit mw: MessageWriter[T]): ActorRef = {
    Source
      .actorRef(bufferSize, overflowStrategy)
      .map[String](mw.write)
      .to(SnsPublisher.sink(settings.topicArn))
      .run()
  }

}
