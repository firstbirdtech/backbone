package backbone

import java.net.URI

import akka.Done
import akka.stream.scaladsl.Source
import backbone.format.DefaultMessageWrites
import backbone.publisher.PublisherSettings
import backbone.scaladsl.Backbone
import backbone.testutil.{BaseTest, MockSNSAsyncClient, TestActorSystem}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.Future
import scala.concurrent.duration._

class BackbonePublishSpec
    extends FixtureAnyWordSpec
    with BaseTest
    with TestActorSystem
    with MockSNSAsyncClient
    with DefaultMessageWrites {

  "Backbone.publishAsync" should {

    "publish a single message to an SNS topic" in { ctx =>
      import ctx._

      val result = backbone.publishAsync("message", publisherSettings)

      whenReady(result) { res =>
        res mustBe Done

        val req = PublishRequest
          .builder()
          .topicArn(publisherSettings.topicArn)
          .message("message")
          .build()

        verify(snsClient).publish(req)
      }
    }

    "publish multiple messages to an SNS topic" in { ctx =>
      import ctx._

      val messages = "message-1" :: "message-2" :: Nil
      val result   = backbone.publishAsync(messages, publisherSettings)

      whenReady(result) { res =>
        res mustBe Done

        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-1").build()
        )
        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from an ActorRef" in { ctx =>
      import ctx._

      val actorRef = backbone.actorPublisher[String](publisherSettings)

      actorRef ! "message-1"
      expectNoMessage(100.millis)
      actorRef ! "message-2"
      expectNoMessage(100.millis)

      within(100.millis) {
        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-1").build()
        )
        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from a Sink" in { ctx =>
      import ctx._

      val sink = backbone.publisherSink[String](publisherSettings)

      val result: Future[Done] = Source("message-1" :: "message-2" :: Nil)
        .runWith(sink)

      whenReady(result) { res =>
        res mustBe Done

        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-1").build()
        )
        verify(snsClient).publish(
          PublishRequest.builder().topicArn(publisherSettings.topicArn).message("message-2").build()
        )
      }
    }

  }

  case class FixtureParam(backbone: Backbone, publisherSettings: PublisherSettings)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val sqsClient = SqsAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      .region(Region.EU_CENTRAL_1)
      .endpointOverride(URI.create("http://localhost:9342"))
      .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
      .build()

    val backbone          = Backbone()
    val publisherSettings = PublisherSettings("topic-arn")
    val param             = FixtureParam(backbone, publisherSettings)

    super.withFixture(test.toNoArgTest(param))
  }

}
