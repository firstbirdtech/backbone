package backbone.generic.json

import akka.actor.ActorSystem
import backbone.generic.json.Implicits._
import backbone.aws.Implicits._
import backbone.consumer.CountLimitation
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings, SnsEnvelope}
import backbone.testutil._
import com.amazonaws.services.sqs.model.Message
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._

class GenericFormatSpec
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

    val envelope = SnsEnvelope("subject", """{"a":"asdf"}""")

    val message = new Message()
      .withBody(Json.toJson(envelope).toString())

    "request messages form the queue url returned when creating the queue" in withMessages(message :: Nil) {


      case class EventA(a: String)

      val settings = ConsumerSettings("subject" :: Nil, Nil, "queue-name", 1, CountLimitation(1))
      val backbone = Backbone()

      val f = backbone.consume[EventA](settings){ ea =>
        Consumed
      }

      Await.ready(f, 5.seconds)
    }
  }
}
