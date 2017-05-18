package backbone

import akka.Done
import akka.stream.scaladsl.Source
import backbone.consumer.ConsumerSettings
import backbone.format.DefaultMessageWrites
import backbone.publisher.PublisherSettings
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{MockSNSAsyncClient, PublishHandler, TestActorSystem}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{fixture, MustMatchers, Outcome}

import scala.concurrent.Future
import scala.concurrent.duration._

class BackboneSpec
    extends fixture.WordSpec
    with MockitoSugar
    with ScalaFutures
    with MustMatchers
    with TestActorSystem
    with MockSNSAsyncClient
    with DefaultMessageWrites {

  "Backbone.consume" should {
    // TODO: consume tests

    "doe something" in { ctx =>
      import ctx._

      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")

      backbone.consume[String](settings) { s =>
        Consumed
      }
    }

  }

  "Backbone.publishAsync" should {

    "publish a single message to an SNS topic" in { ctx =>
      import ctx._

      val result = backbone.publishAsync("message", publisherSettings)

      whenReady(result) { res =>
        res mustBe Done
        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message")),
                                       any[PublishHandler])
      }
    }

    "publish multiple messages to an SNS topic" in { ctx =>
      import ctx._

      val messages = "message-1" :: "message-2" :: Nil
      val result   = backbone.publishAsync(messages, publisherSettings)

      whenReady(result) { res =>
        res mustBe Done
        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-1")),
                                       any[PublishHandler])
        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-2")),
                                       any[PublishHandler])
      }
    }

    "publish messages from an ActorRef" in { ctx =>
      import ctx._

      val actorRef = backbone.actorPublisher[String](publisherSettings)

      within(1.seconds) {
        actorRef ! "message-1"
        expectNoMsg
        actorRef ! "message-2"
        expectNoMsg

        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-1")),
                                       any[PublishHandler])
        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-2")),
                                       any[PublishHandler])
      }
    }

    "publish messages from a Sink" in { ctx =>
      import ctx._

      val sink = backbone.publisherSink[String](publisherSettings)

      val result: Future[Done] = Source("message-1" :: "message-2" :: Nil)
        .runWith(sink)

      whenReady(result) { res =>
        res mustBe Done

        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-1")),
                                       any[PublishHandler])
        verify(snsClient).publishAsync(meq(new PublishRequest(publisherSettings.topicArn, "message-2")),
                                       any[PublishHandler])
      }
    }

  }

  case class FixtureParam(backbone: Backbone, publisherSettings: PublisherSettings)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val sqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
      .standard()
      .withEndpointConfiguration(new EndpointConfiguration("http://localhost:9324", "eu-central-1"))
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
      .build()

    val backbone          = Backbone()
    val publisherSettings = PublisherSettings("topic-arn")
    val param             = FixtureParam(backbone, publisherSettings)

    super.withFixture(test.toNoArgTest(param))
  }

}
