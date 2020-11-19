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
