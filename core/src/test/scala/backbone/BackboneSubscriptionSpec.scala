package backbone

import akka.stream.alpakka.sqs.{MessageAttributeName, MessageSystemAttributeName}
import backbone.consumer.{ConsumerSettings, CountLimitation, ReceiveSettings}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil._
import cats.implicits._
import io.circe.syntax._
import org.mockito.Mockito
import org.mockito.captor.ArgCaptor
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.services.sns.model.SubscribeRequest
import software.amazon.awssdk.services.sqs.model.{
  CreateQueueRequest,
  Message,
  QueueAttributeName,
  ReceiveMessageRequest
}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class BackboneSubscriptionSpec
    extends AnyWordSpec
    with BaseTest
    with TestActorSystem
    with MockSNSAsyncClient
    with MockSQSAsyncClient {

  "Backbone.consume" should {

    "subscribe the queue with it's arn to the provided topics" in {

      val settings = ConsumerSettings("topic-arn" :: "topic-arn-2" :: Nil, "Queue-name", None, 1, Some(CountLimitation(0)))
      val backbone = Backbone()

      val result = backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        val request = SubscribeRequest
          .builder()
          .topicArn("topic-arn")
          .protocol("sqs")
          .endpoint("queue-arn")
          .build()

        verify(snsClient).subscribe(request)
      }
    }

    "create a queue with the configured name" in {
      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(0)))
      val backbone = Backbone()

      val result = backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        verify(sqsClient).createQueue(CreateQueueRequest.builder().queueName("queue-name").build())
      }
    }

    "create an encrypted queue with the configured name and kms key alias" in {
      val settings = ConsumerSettings(
        Nil,
        "queue-name",
        "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some,
        1,
        Some(CountLimitation(0))
      )
      val backbone = Backbone()

      val result = backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        val request = CreateQueueRequest
          .builder()
          .queueName("queue-name")
          .attributes(
            Map(QueueAttributeName.KMS_MASTER_KEY_ID -> "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias").asJava
          )
          .build()

        verify(sqsClient).createQueue(request)
      }
    }

    val envelope = SnsEnvelope("message")
    val message  = Message.builder().body(envelope.asJson.toString()).build()

    "request messages form the queue url returned when creating the queue" in withMessages(message :: Nil) {
      val receiveSettings = ReceiveSettings()
        .withAttribute(MessageSystemAttributeName.senderId)
        .withMessageAttribute(MessageAttributeName("TestAttribute"))
        .withVisibilityTimeout(1.minute)

      val settings =
        ConsumerSettings("subject" :: Nil, "queue-name", None, 1, Some(CountLimitation(1)), receiveSettings)

      val backbone = Backbone()

      val f = backbone.consume[String](settings)(_ => Consumed)
      Await.ready(f, 5.seconds)

      val captor = ArgCaptor[ReceiveMessageRequest]

      verify(sqsClient, Mockito.atLeastOnce()).receiveMessage(captor)
      val request = captor.value

      request.maxNumberOfMessages() mustBe 10
      request.waitTimeSeconds() mustBe 20
      request.attributeNamesAsStrings() mustBe List("SenderId").asJava
      request.messageAttributeNames() mustBe List("TestAttribute").asJava
      request.visibilityTimeout() mustBe 60
      request.queueUrl() mustBe "queue-url"
    }
  }
}
