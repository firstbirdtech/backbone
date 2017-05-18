package backbone.publisher

import akka.Done
import akka.stream.OverflowStrategy
import backbone.format.DefaultMessageWrites
import backbone.testutil.{MockSNSAsyncClient, PublishHandler, TestActorSystem}
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}

import scala.concurrent.duration._

class PublisherSpec
    extends WordSpec
    with TestActorSystem
    with MustMatchers
    with ScalaFutures
    with MockSNSAsyncClient
    with DefaultMessageWrites {

  "Publisher" should {

    "publish a list of messages" in {
      val settings = Publisher.Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      val result = new Publisher(settings).publishAsync(messages)

      whenReady(result) { res =>
        res mustBe Done
        verify(snsClient).publishAsync(meq(new PublishRequest(settings.topicArn, "message-1")), any[PublishHandler])
        verify(snsClient).publishAsync(meq(new PublishRequest(settings.topicArn, "message-2")), any[PublishHandler])
      }
    }

    "publish messages from an ActorRef" in {
      val settings = Publisher.Settings("topic-arn")
      val actorRef = new Publisher(settings).actor[String](Int.MaxValue, OverflowStrategy.dropHead)

      actorRef ! "message-1"
      expectNoMsg
      actorRef ! "message-2"
      expectNoMsg

      within(100.millis) {

        verify(snsClient).publishAsync(meq(new PublishRequest(settings.topicArn, "message-1")), any[PublishHandler])
        verify(snsClient).publishAsync(meq(new PublishRequest(settings.topicArn, "message-2")), any[PublishHandler])
      }
    }
  }

}
