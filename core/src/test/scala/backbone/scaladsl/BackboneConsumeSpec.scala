package backbone.scaladsl

import backbone.consumer.DefaultMessageReaders.stringFormat
import backbone.consumer.{ConsumerSettings, CountLimitation, JsonReader}
import backbone.testutil.Helpers._
import backbone.testutil.{BaseTest, ElasticMQ, TestActorSystem}
import backbone.{Consumed, MessageReader, Rejected}
import cats.syntax.all._
import io.circe.syntax._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model._
import software.amazon.awssdk.services.sqs.model._

import java.util.concurrent.CompletableFuture
import scala.compat.java8.FutureConverters._
import scala.util.Success

class BackboneConsumeSpec extends FixtureAnyWordSpec with BaseTest with ElasticMQ with TestActorSystem {

  "Backbone.consume" should {

    "create a queue with the configured name" in { f =>
      val queueName = "queue-name-1"
      val settings  = ConsumerSettings(Nil, queueName, None, 1, Some(CountLimitation(0)))

      val result = for {
        _ <- f.backbone.consume[String](settings)(_ => Consumed)
        r <- sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).toScala
      } yield r

      whenReady(result) { res => res.queueUrl mustBe s"$elasticMqHost/000000000000/$queueName" }
    }

    "create an encrypted queue with the configured name and kms key alias" in { f =>
      val queueName = "queue-name-2"
      val settings = ConsumerSettings(
        Nil,
        queueName,
        "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some,
        1,
        Some(CountLimitation(0))
      )

      val result = for {
        _ <- f.backbone.consume[String](settings)(_ => Consumed)
        r <- sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).toScala
      } yield r

      whenReady(result) { res => res.queueUrl mustBe s"$elasticMqHost/000000000000/$queueName" }
    }

    "fail parsing a wrongly formatted message and keep in on the queue" in { f =>
      val queueName = "no-visibility"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = ConsumerSettings(Nil, queueName, None, 1, Some(CountLimitation(1)))

      val result = for {
        _ <- createQueue(queueName, Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "0"))
        _ <- sendMessage("blabla", queueUrl)
        _ <- f.backbone.consume[String](settings)(_ => Consumed)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 1 }
    }

    "consume messages from the queue url" in { f =>
      val queueName = "test-queue"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = ConsumerSettings(Nil, queueName, None, 1, Some(CountLimitation(1)))
      val msg      = JsonReader.SnsEnvelope("message")

      val result = for {
        _ <- createQueue(queueName)
        _ <- sendMessage(msg.asJson.toString, queueUrl)
        _ <- f.backbone.consume[String](settings)(_ => Consumed)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

    "consume messages from the queue url if the MessageReader returns no event" in { f =>
      val queueName = "test-queue"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = ConsumerSettings(Nil, queueName, None, 1, Some(CountLimitation(1)))
      val reader   = MessageReader(_ => Success(Option.empty[String]))
      val msg      = JsonReader.SnsEnvelope("message")

      val result = for {
        _ <- createQueue(queueName)
        _ <- sendMessage(msg.asJson.toString, queueUrl)
        _ <- f.backbone.consume[String](settings)(_ => Rejected)(reader)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

    "reject messages from the queue" in { f =>
      val queueName = "no-visibility"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = ConsumerSettings(Nil, queueName, None, 1, Some(CountLimitation(1)))
      val msg      = JsonReader.SnsEnvelope("message")

      val result = for {
        _ <- createQueue(queueName, Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "0"))
        _ <- sendMessage(msg.asJson.toString, queueUrl)
        _ <- f.backbone.consume[String](settings)(_ => Rejected)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 1 }
    }
  }

  case class FixtureParam(backbone: Backbone)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val snsClient = mock(classOf[SnsAsyncClient])

    when(snsClient.subscribe(any(classOf[SubscribeRequest]))).thenReturn {
      val response = SubscribeResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val backbone = Backbone()

    val fixture = FixtureParam(backbone)
    super.withFixture(test.toNoArgTest(fixture))
  }

}
