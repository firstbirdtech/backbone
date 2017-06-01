package backbone

import akka.Done
import backbone.consumer.{ConsumerSettings, CountLimitation, SourceSettingsSqs}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{ElasticMQ, MockSNSAsyncClient, TestActorSystem}
import com.amazonaws.services.sqs.model.{CreateQueueRequest, Message, SendMessageRequest}
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.concurrent.duration._

class BackboneConsumeSpec
    extends WordSpec
    with ElasticMQ
    with MockSNSAsyncClient
    with MockitoSugar
    with ScalaFutures
    with MustMatchers
    with TestActorSystem {

  val backbone = Backbone()

  "Backbone.consume" should {

    "create a queue with the configured name" in {

      val settings = ConsumerSettings(Nil, "queue-name-1", 1, CountLimitation(0))

      whenReady(consume(settings)) { res =>
        sqsClient.getQueueUrl("queue-name-1").getQueueUrl must be("http://localhost:9324/queue/queue-name-1")
      }

    }

    "fail parsing a wrongly formatted message and keep in on the queue" in {

      val message            = new Message().withBody("blabla")
      val attributes         = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest("no-visibility").withAttributes(attributes.asJava)

      sqsClient.createQueue(createQueueRequest)
      sqsClient.sendMessage(new SendMessageRequest("http://localhost:9324/queue/no-visibility", message.getBody))

      val settings = ConsumerSettings(Nil, "no-visibility", 1, CountLimitation(1))

      whenReady(consume(settings)) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }

    }

    "consume messages from the queue url" in {
      sendMessage("subject", "message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", 1, CountLimitation(1))

      whenReady(consume(settings)) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }

    }
    "reject messages from the queue" in {
      sendMessage("subject", "message", "no-visibility")

      val settings = ConsumerSettings(Nil, "no-visibility", 1, CountLimitation(0), SourceSettingsSqs(0, 100, 10))

      whenReady(reject(settings)) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1

      }
    }
  }

  private[this] def consume(settings: ConsumerSettings): Future[Done] = {
    val f = backbone.consume[String](settings)(s => Consumed)
    f
  }
  private[this] def reject(settings: ConsumerSettings): Future[Done] = {
    val f = backbone.consume[String](settings)(s => Rejected)
    f
  }

  private[this] def sendMessage(subject: String, message: String, queue: String): Unit = {
    val envelope = SnsEnvelope(subject, message)

    val sqsMessage = new Message()
      .withBody(envelope.asJson.toString())
    sqsClient.sendMessage(new SendMessageRequest(s"http://localhost:9324/queue/$queue", sqsMessage.getBody))
  }
}
