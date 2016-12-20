import akka.actor.ActorSystem
import backbone.format.Format
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings, Rejected}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import org.scalatest.WordSpec

import scala.concurrent.Future
import scala.util.{Success, Try}

class BackboneSpec extends WordSpec{


  "consume" should {

    "do something" in {
      implicit val sqs = new AmazonSQSAsyncClient()
      implicit val sns = new AmazonSNSAsyncClient()
      implicit val actorSystem = ActorSystem()

      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")
      val backbone = Backbone()

      implicit val format = new Format[Event]{
        override def read(s: String): Try[Event] = Success(EventA("event-a"))
      }

      backbone.consume[Event](settings){
        case EventA(_) => Consumed
        case EventB(_) => Rejected
      }

      backbone.consume[Event](1, settings) {
        case EventA(content) => Future.successful(Consumed)
        case EventB(content) => Future.successful(Rejected)
      }



    }

  }

  sealed trait Event
  case class EventA(content: String) extends Event
  case class EventB(content: String) extends Event


}
