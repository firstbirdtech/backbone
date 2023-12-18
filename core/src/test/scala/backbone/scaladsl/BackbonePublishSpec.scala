package backbone.scaladsl

import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Source
import backbone.publisher.{DefaultMessageWriters, PublisherSettings}
import backbone.scaladsl.Backbone
import backbone.testutil.{BaseTest, TestActorSystem}
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model._
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import java.util.concurrent.CompletableFuture
import scala.concurrent.duration._

class BackbonePublishSpec extends FixtureAnyWordSpec with BaseTest with TestActorSystem with DefaultMessageWriters {

  "Backbone.publishAsync" should {

    "publish a single message to an SNS topic" in { f =>
      val settings = PublisherSettings("topic-arn")
      val result   = f.backbone.publishAsync("message", settings)

      whenReady(result) { res =>
        res mustBe Done

        val req = PublishRequest
          .builder()
          .topicArn(settings.topicArn)
          .message("message")
          .build()

        verify(f.snsClient).publish(req)
      }
    }

    "publish multiple messages to an SNS topic" in { f =>
      val messages = "message-1" :: "message-2" :: Nil
      val settings = PublisherSettings("topic-arn")
      val result   = f.backbone.publishAsync(messages, settings)

      whenReady(result) { res =>
        res mustBe Done

        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from an ActorRef" in { f =>
      val settings = PublisherSettings("topic-arn")
      val actorRef = f.backbone.actorPublisher[String](settings)

      actorRef ! "message-1"
      expectNoMessage(100.millis)
      actorRef ! "message-2"
      expectNoMessage(100.millis)

      within(100.millis) {
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from a Sink" in { f =>
      val settings = PublisherSettings("topic-arn")
      val sink     = f.backbone.publisherSink[String](settings)

      val result = Source("message-1" :: "message-2" :: Nil).runWith(sink)

      whenReady(result) { res =>
        res mustBe Done

        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

  }

  case class FixtureParam(backbone: Backbone, snsClient: SnsAsyncClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val sqsClient = mock[SqsAsyncClient]
    implicit val snsClient = mock[SnsAsyncClient]

    when(snsClient.publish(*[PublishRequest])).thenReturn {
      val response = PublishResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val backbone = Backbone()
    val param    = FixtureParam(backbone, snsClient)

    super.withFixture(test.toNoArgTest(param))
  }

}
