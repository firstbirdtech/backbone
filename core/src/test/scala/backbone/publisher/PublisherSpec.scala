package backbone.publisher

import akka.Done
import akka.stream.OverflowStrategy
import backbone.format.DefaultMessageWrites
import backbone.testutil.{BaseTest, MockSNSAsyncClient, TestActorSystem}
import org.mockito.Mockito
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.services.sns.model.PublishRequest

import scala.concurrent.duration._

class PublisherSpec
    extends AnyWordSpec
    with BaseTest
    with TestActorSystem
    with MockSNSAsyncClient
    with DefaultMessageWrites {

  "Publisher" should {

    "publish a list of messages" in {
      val settings = Publisher.Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      val result = new Publisher(settings).publishAsync(messages)

      whenReady(result) { res =>
        res mustBe Done
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
      }
    }

    "publish messages from an ActorRef" in {
      val settings = Publisher.Settings("topic-arn")
      val actorRef = new Publisher(settings).actor[String](Int.MaxValue, OverflowStrategy.dropHead)

      actorRef ! "message-1"
      expectNoMessage(100.millis)
      actorRef ! "message-2"
      expectNoMessage(100.millis)

      within(100.millis) {
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
      }
    }

    "restarts publisher sink in case of a failure" in {
      val settings = Publisher.Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      Mockito
        .doThrow(new RuntimeException("publish exception"))
        .when(snsClient)
        .publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())

      val result = new Publisher(settings).publishAsync(messages)

      whenReady(result) { res =>
        res mustBe Done
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
      }

    }

  }

}
