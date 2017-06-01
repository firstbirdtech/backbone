package backbone

import akka.actor.ActorSystem
import backbone.consumer.{ConsumerSettings, CountLimitation}
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest}
import io.circe.syntax._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class BackboneSubscriptionSpec
    extends WordSpec
    with MockitoUtils
    with BeforeAndAfterAll
    with MockSNSAsyncClient
    with MockSQSAsyncClient
    with Matchers {

  private[this] implicit val system = ActorSystem()

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
  }

  "Backbone.consume" should {

    "subscribe the queue with it's arn to the provided topics" in {

      val settings = ConsumerSettings("topic-arn" :: Nil, "Queue-name", 1, CountLimitation(0))
      val backbone = Backbone()

      val f = backbone.consume[String](settings)(_ => Consumed)
      Await.ready(f, 5.seconds)

      verify(snsClient).subscribeAsync(
        meq("topic-arn"),
        meq("sqs"),
        meq("queue-arn"),
        any[AsyncHandler[SubscribeRequest, SubscribeResult]]
      )
    }

    "create a queue with the configured name" in {
      val settings = ConsumerSettings(Nil, "queue-name", 1, CountLimitation(0))
      val backbone = Backbone()

      val f = backbone.consume[String](settings)(_ => Consumed)
      Await.ready(f, 5.seconds)

      verify(sqsClient).createQueueAsync(meq("queue-name"), any[CreateQueueHandler])
    }

    val envelope = SnsEnvelope("subject", "message")

    val message = new Message()
      .withBody(envelope.asJson.toString())

    "request messages form the queue url returned when creating the queue" in withMessages(message :: Nil) {

      val settings = ConsumerSettings("subject" :: Nil, "queue-name", 1, CountLimitation(1))
      val backbone = Backbone()

      val f = backbone.consume[String](settings)(s => Consumed)
      Await.ready(f, 5.seconds)

      val captor = argumentCaptor[ReceiveMessageRequest]

      verify(sqsClient, Mockito.atLeastOnce()).receiveMessageAsync(captor.capture(), any[ReceiveMessagesHandler])
      val request = captor.getValue

      request.getMaxNumberOfMessages shouldBe 10
      request.getWaitTimeSeconds shouldBe 20
      request.getQueueUrl shouldBe "queue-url"
    }
  }
}
