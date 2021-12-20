package backbone.consumer.scaladsl

import backbone.consumer.{CountLimitation, JsonReader, Settings, _}
import backbone.testutil.Helpers._
import backbone.testutil.{BaseTest, ElasticMQ, TestActorSystem}
import backbone.{Consumed, MessageReader, Rejected}
import io.circe.syntax._
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.services.sqs.model._

import scala.concurrent.Future
import scala.util.Success

class ConsumerSpec extends AnyWordSpec with BaseTest with ElasticMQ with TestActorSystem {

  "consumeAsync" should {

    "fail parsing a wrongly formatted message and keep in on the queue" in {
      val queueName = "no-visibility"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = Settings(queueUrl, limitation = Some(CountLimitation(1)))
      val consumer = Consumer(testJsonReader)

      val result = for {
        _ <- createQueue(queueName, Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "0"))
        _ <- sendMessage("blabla", queueUrl)
        _ <- consumer.consumeAsync[String](settings)(_ => Future.successful(Consumed))
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 1 }
    }

    "consume messages from the queue url" in {
      val queueName = "test-queue"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = Settings(queueUrl, limitation = Some(CountLimitation(1)))
      val consumer = Consumer(testJsonReader)

      val msg = JsonReader.SnsEnvelope("message")

      val result = for {
        _ <- createQueue(queueName)
        _ <- sendMessage(msg.asJson.toString, queueUrl)
        _ <- consumer.consumeAsync[String](settings)(_ => Future.successful(Consumed))
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

    "consume messages from the queue url if the MessageReader returns no event" in {
      val queueName = "test-queue"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val reader   = MessageReader(_ => Success(Option.empty[String]))
      val settings = Settings(queueUrl, limitation = Some(CountLimitation(1)))
      val consumer = Consumer(testJsonReader)

      val msg = JsonReader.SnsEnvelope("message")

      val result = for {
        _ <- createQueue(queueName)
        _ <- sendMessage(msg.asJson.toString, queueUrl)
        _ <- consumer.consumeAsync[String](settings)(_ => Future.successful(Rejected))(reader)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res => res.messages() must have size 0 }
    }

//    "reject messages from the queue" in {
//      val queueName = "no-visibility"
//      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"
//
//      val settings = Settings(queueUrl, limitation = Some(CountLimitation(1)))
//      val consumer = Consumer(testJsonReader)
//
//      val msg = JsonReader.SnsEnvelope("message")
//
//      val result = for {
//        _ <- createQueue(queueName, Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "0"))
//        _ <- sendMessage(msg.asJson.toString, queueUrl)
//        _ <- consumer.consumeAsync[String](settings)(_ => Future.successful(Rejected))
//        r <- receiveMessage(queueUrl)
//      } yield r
//
//      whenReady(result) { res => res.messages() must have size 1 }
//    }
  }

}
