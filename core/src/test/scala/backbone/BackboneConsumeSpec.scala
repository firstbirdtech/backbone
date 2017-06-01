package backbone

import akka.Done
import backbone.consumer.{ConsumerSettings, CountLimitation}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{ElasticMQ, MockSNSAsyncClient}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import com.amazonaws.services.sqs.model.{CreateQueueRequest, Message, SendMessageRequest}
import io.circe.syntax._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class BackboneConsumeSpec
    extends WordSpec
    with ElasticMQ
    with MockSNSAsyncClient
    with MockitoSugar
    with ScalaFutures
    with MustMatchers
    with DefaultTestContext {

  val backbone = Backbone()

  "Backbone.consume" should {

    "subscribe the queue with it's arn to the provided topics" in {

      val settings =
        ConsumerSettings("topic-arn" :: Nil, "queue-name", 1, CountLimitation(0))

      val f = backbone.consume[String](settings)(_ => Consumed)
      Await.ready(f, 5.seconds)

      verify(snsClient).subscribeAsync(
        meq("topic-arn"),
        meq("sqs"),
        meq("arn:aws:sqs:elasticmq:000000000000:queue-name"),
        any[AsyncHandler[SubscribeRequest, SubscribeResult]]
      )
    }

    "create a queue with the configured name" in {
      val settings = ConsumerSettings(Nil, "queue-name-1", 1, CountLimitation(0))

      whenReady(consume(settings)) { res =>
        res mustBe Done
        sqsClient.getQueueUrl("queue-name-1").getQueueUrl must be("http://localhost:9324/queue/queue-name-1")
      }

    }

    "fail parsing a wrongly formatted message and keep in on the queue" in {

      val message = new Message()
        .withBody("blabla")
      sqsClient.createQueue(
        new CreateQueueRequest("no-visibility").withAttributes(HashMap("VisibilityTimeout" -> "0").asJava))
      sqsClient.sendMessage(new SendMessageRequest("http://localhost:9324/queue/no-visibility", message.getBody))

      val settings =
        ConsumerSettings(Nil, "no-visibility", 1, CountLimitation(1))
      whenReady(consume(settings)) { res =>
        res mustBe Done
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }

    }

    "consume messages from the queue url" in {
      sendMessage("subject", "message", "queue-name")

      val settings =
        ConsumerSettings(Nil, "queue-name", 1, CountLimitation(1))

      whenReady(consume(settings)) { res =>
        res mustBe Done
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }

    }

    "remove messages from the queue with a unwanted topic" in {

      sendMessage("notSubject", "message", "queue-name")

      val settings =
        ConsumerSettings(Nil, "queue-name", 1, CountLimitation(0), 0, 100, 10)

      whenReady(consume(settings)) { res =>
        res mustBe Done
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }
    }
  }

  private def consume(settings: ConsumerSettings): Future[Done] = {
    val f = backbone.consume[String](settings)(s => Consumed)
    f
  }

  private def sendMessage(subject: String, message: String, queue: String): Unit = {
    val envelope = SnsEnvelope(subject, message)

    val sqsMessage = new Message()
      .withBody(envelope.asJson.toString())
    sqsClient.sendMessage(new SendMessageRequest(s"http://localhost:9324/queue/$queue", sqsMessage.getBody))
  }
}
