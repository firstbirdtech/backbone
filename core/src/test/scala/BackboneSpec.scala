import akka.actor.ActorSystem
import backbone.format.Format
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings, Rejected}
import com.amazonaws.ClientConfiguration
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.collection.mutable

class BackboneSpec extends WordSpec with Matchers {

  "consume" should {

    "do something" ignore {
      implicit val sqs         = new AmazonSQSAsyncClient()
      implicit val sns         = new AmazonSNSAsyncClient()
      implicit val actorSystem = ActorSystem()

      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")
      val backbone = Backbone()

      implicit val format = new Format[Event] {
        override def read(s: String): Try[Event] = Success(EventA("event-a"))
      }

      backbone.consume[Event](settings) {
        case EventA(_) => Consumed
        case EventB(_) => Rejected
      }

      backbone.consumeAsync[Event](settings) {
        case EventA(content) => Future.successful(Consumed)
        case EventB(content) => Future.successful(Rejected)
      }

    }

    "consume a single string " in {

      implicit val sqs = new AmazonSQSAsyncClient()
        .withEndpoint[AmazonSQSAsyncClient]("http://sqs:9324")

      implicit val sns: AmazonSNSAsyncClient = new AmazonSNSAsyncClient()
        .withEndpoint[AmazonSNSAsyncClient]("http://localhost:9911")

      val queue = sqs.createQueue("test-queue")

      val topic = sns.createTopic("test-topic")

      val subscription = sns.subscribe(topic.getTopicArn, "sqs", "aws-sqs://test-queue?amazonSQSEndpoint=http://sqs:9324&accessKey=&secretKey=")

      sns.publish(topic.getTopicArn, "message")

      sqs.sendMessage(queue.getQueueUrl, "message2")
      val messages = sqs.receiveMessage(queue.getQueueUrl)


      val a = "a"
//      sns.subscribe(response.getTopicArn,"sqs",)
//      implicit val actorSystem = ActorSystem()
//
//      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")
//      val backbone = Backbone()
//
//      implicit val format = new Format[String]{
//        override def read(s: String): Try[String] = Success(s)
//      }
//
//      val queue = mutable.Queue[String]()
//
//      backbone.consume[String](settings){ s =>
//        queue += s
//        Consumed
//      }
//
//      queue should contain("asdf")
    }

  }

  sealed trait Event
  case class EventA(content: String) extends Event
  case class EventB(content: String) extends Event

}
