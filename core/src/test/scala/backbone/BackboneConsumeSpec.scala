package backbone

import akka.stream.alpakka.sqs.SqsSourceSettings
import backbone.consumer.{ConsumerSettings, CountLimitation}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{BaseTest, ElasticMQ, MockSNSAsyncClient, TestActorSystem}
import cats.syntax.all._
import io.circe.syntax._
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.services.sqs.model._

import scala.collection.immutable.HashMap
import scala.compat.java8.FutureConverters._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Success

class BackboneConsumeSpec
    extends AnyWordSpec
    with BaseTest
    with ElasticMQ
    with MockSNSAsyncClient
    with TestActorSystem {

  private[this] val backbone = Backbone()

  "Backbone.consume" should {

    "create a queue with the configured name" in {

      val settings = ConsumerSettings(Nil, "queue-name-1", None, 1, Some(CountLimitation(0)))

      val result = for {
        _ <- backbone.consume[String](settings)(_ => Consumed)
        r <- sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue-name-1").build()).toScala
      } yield r

      whenReady(result) { res => res.queueUrl must be("http://localhost:9324/000000000000/queue-name-1") }
    }

    "create an encrypted queue with the configured name and kms key alias" in {

      val settings = ConsumerSettings(
        Nil,
        "queue-name-2",
        "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some,
        1,
        Some(CountLimitation(0))
      )

      val result = for {
        _ <- backbone.consume[String](settings)(_ => Consumed)
        r <- sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue-name-2").build()).toScala
      } yield r

      whenReady(result) { res => res.queueUrl must be("http://localhost:9324/000000000000/queue-name-2") }
    }

    "fail parsing a wrongly formatted message and keep in on the queue" in {

      val message    = Message.builder().body("blabla").build()
      val attributes = HashMap(QueueAttributeName.VISIBILITY_TIMEOUT -> "0")
      val createQueueRequest = CreateQueueRequest
        .builder()
        .queueName("no-visibility")
        .attributes(attributes.asJava)
        .build()

      val sendMessageRequest = SendMessageRequest
        .builder()
        .queueUrl("http://localhost:9324/queue/no-visibility")
        .messageBody(message.body)
        .build()

      val receiveMessageRequest = ReceiveMessageRequest
        .builder()
        .queueUrl("http://localhost:9324/queue/no-visibility")
        .build()

      val settings = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(1)))

      val result = for {
        _ <- sqsClient.createQueue(createQueueRequest).toScala
        _ <- sqsClient.sendMessage(sendMessageRequest).toScala
        _ <- backbone.consume[String](settings)(_ => Consumed)
        r <- sqsClient.receiveMessage(receiveMessageRequest).toScala
      } yield r

      whenReady(result) { res => res.messages() must have size 1 }
    }

    "consume messages from the queue url" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))

      val result = for {
        _ <- backbone.consume[String](settings)(_ => Consumed)
        r <-
          sqsClient
            .receiveMessage(ReceiveMessageRequest.builder().queueUrl("http://localhost:9324/queue/queue-name").build())
            .toScala
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

    "consume messages from the queue url if the MessageReader returns no event" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))
      val reader   = MessageReader(_ => Success(Option.empty[String]))

      val result = for {
        _ <- backbone.consume[String](settings)(_ => Rejected)(reader)
        r <-
          sqsClient
            .receiveMessage(ReceiveMessageRequest.builder().queueUrl("http://localhost:9324/queue/queue-name").build())
            .toScala
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

    "reject messages from the queue" in {
      sendMessage("message", "no-visibility")

      val receiveSettings = SqsSourceSettings().withWaitTime(10.seconds).withMaxBufferSize(100).withMaxBatchSize(10)
      val settings        = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(0)), receiveSettings)

      val result = for {
        _ <- backbone.consume[String](settings)(_ => Rejected)
        r <-
          sqsClient
            .receiveMessage(
              ReceiveMessageRequest.builder().queueUrl("http://localhost:9324/queue/no-visibility").build())
            .toScala
      } yield r

      whenReady(result) { res => res.messages() must have size 1 }
    }
  }

  private[this] def sendMessage(message: String, queue: String): Unit = {
    val envelope   = SnsEnvelope(message)
    val sqsMessage = Message.builder().body(envelope.asJson.toString).build()
    val request = SendMessageRequest
      .builder()
      .queueUrl(s"http://localhost:9324/queue/$queue")
      .messageBody(sqsMessage.body)
      .build()

    sqsClient.sendMessage(request)
  }
}
