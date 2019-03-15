package backbone

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import akka.Done
import akka.stream.scaladsl.Flow
import backbone.consumer.{ConsumerSettings, CountLimitation, MessageContext, ReceiveSettings}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{ElasticMQ, MockSNSAsyncClient, TestActorSystem}
import cats.implicits._
import com.amazonaws.services.sqs.model.{CreateQueueRequest, Message, SendMessageRequest}
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.util.Success

class BackboneConsumeSpec
  extends WordSpec
    with ElasticMQ
    with MockSNSAsyncClient
    with MockitoSugar
    with ScalaFutures
    with MustMatchers
    with TestActorSystem {

  override implicit def patienceConfig: PatienceConfig = super.patienceConfig.copy(timeout = Span(3, Seconds))

  val backbone = Backbone()

  "Backbone.consume" should {

    "create a queue with the configured name" in {

      val settings = ConsumerSettings(Nil, "queue-name-1", None, 1, Some(CountLimitation(0)))

      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { res =>
        sqsClient.getQueueUrl("queue-name-1").getQueueUrl must be("http://localhost:9324/queue/queue-name-1")
      }

    }

    "create an encrypted queue with the configured name and kms key alias" in {

      val settings = ConsumerSettings(Nil, "queue-name-2", "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some, 1, Some(CountLimitation(0)))

      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { res =>
        sqsClient.getQueueUrl("queue-name-2").getQueueUrl must be("http://localhost:9324/queue/queue-name-2")
      }

    }

    "fail parsing a wrongly formatted message and keep in on the queue" in {

      val message = new Message().withBody("blabla")
      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest("no-visibility").withAttributes(attributes.asJava)

      sqsClient.createQueue(createQueueRequest)
      sqsClient.sendMessage(new SendMessageRequest("http://localhost:9324/queue/no-visibility", message.getBody))

      val settings = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(1)))
      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }

    }

    "consume messages from the queue url" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))
      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }
    }

    "consume messages from the queue url if the MessageReader returns no event" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))
      val reader = MessageReader(_ => Success(Option.empty[String]))

      val f: Future[Done] = backbone.consume[String](settings)(s => Rejected)(reader)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }
    }

    "reject messages from the queue" in {
      sendMessage("message", "no-visibility")

      val settings = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(0)), ReceiveSettings(0, 100, 10))
      val f: Future[Done] = backbone.consume[String](settings)(s => Rejected)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }
    }

    "consume messages with a preprocessing handler" in {
      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      sendMessage("message1", queueName)
      sendMessage("message2", queueName)
      sendMessage("message3", queueName)

      val settings = ConsumerSettings(Nil, queueName, None, 3, Some(CountLimitation(3)), ReceiveSettings(5, 10, 10))

      val concurrentHashMap = new ConcurrentHashMap[String, String]()

      val f: Future[Done] = backbone.consume[String](settings)(
        s => {
          // store the values we receive in the map
          concurrentHashMap.put(s, s)
          Consumed
        },
        Flow[(MessageContext, String)].map { case (ctx, msg) => (ctx, Some(msg.toUpperCase())) }
      )

      whenReady(f) { _ =>
        // check if the values are there
        concurrentHashMap.get("MESSAGE1") mustBe "MESSAGE1"
        concurrentHashMap.get("MESSAGE2") mustBe "MESSAGE2"
        concurrentHashMap.get("MESSAGE3") mustBe "MESSAGE3"
      }
    }

  }
  "backbone.consumeAsync" should {

    "consume messages with a preprocessing handler" in {
      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      sendMessage("message1", queueName)
      sendMessage("message2", queueName)
      sendMessage("message3", queueName)

      val settings = ConsumerSettings(Nil, queueName, None, 3, Some(CountLimitation(3)), ReceiveSettings(5, 10, 10))

      val concurrentHashMap = new ConcurrentHashMap[String, String]()

      val f: Future[Done] = backbone.consumeAsync[String](settings)(
        s => {
          // store the values we receive in the map
          concurrentHashMap.put(s, s)
          Future.successful(Consumed)
        },
        Flow[(MessageContext, String)].map { case (ctx, msg) => (ctx, Some(msg.toUpperCase())) }
      )

      whenReady(f) { _ =>
        // check if the values are there
        concurrentHashMap.get("MESSAGE1") mustBe "MESSAGE1"
        concurrentHashMap.get("MESSAGE2") mustBe "MESSAGE2"
        concurrentHashMap.get("MESSAGE3") mustBe "MESSAGE3"
      }
    }

  }

  private[this] def sendMessage(message: String, queue: String): String = {
    val envelope = SnsEnvelope(message)

    val sqsMessage = new Message().withBody(envelope.asJson.toString())
    val result = sqsClient.sendMessage(new SendMessageRequest(s"http://localhost:9324/queue/$queue", sqsMessage.getBody))
    result.getMessageId
  }
}
