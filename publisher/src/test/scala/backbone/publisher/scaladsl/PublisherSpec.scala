package backbone.publisher.scaladsl

import akka.Done
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import backbone.publisher.{DefaultMessageWriters, MessageHeaders, Settings}
import backbone.testutil.{BaseTest, TestActorSystem}
import org.mockito.Mockito
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{MessageAttributeValue, PublishRequest, PublishResponse}

import java.util.concurrent.CompletableFuture
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import Mockito._
import org.mockito.ArgumentMatchers

class PublisherSpec extends FixtureAnyWordSpec with BaseTest with TestActorSystem with DefaultMessageWriters {

  "Publisher" should {

    "publish a single message" in { f =>
      val settings = Settings("topic-arn")
      val message  = "message-1"

      val result = f.publisher.publishAsync[String](settings)(message)

      whenReady(result) { res =>
        res mustBe Done
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verifyNoMoreInteractions(f.snsClient)
      }
    }

    "publish a list of messages" in { f =>
      val settings = Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      val result = f.publisher.publishAsync[String](settings)(messages: _*)

      whenReady(result) { res =>
        res mustBe Done
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
        verifyNoMoreInteractions(f.snsClient)
      }
    }

    "publish messages from a Sink" in { f =>
      val settings = Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      val publishSink = f.publisher.sink[String](settings)
      val result      = Source(messages).runWith(publishSink)

      whenReady(result) { res =>
        res mustBe Done
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
        verifyNoMoreInteractions(f.snsClient)
      }
    }

    "publish messages from a Sink including headers" in { f =>
      val settings = Settings("topic-arn")
      val messages = List(
        ("message-1", MessageHeaders.from("header" -> "value1")),
        ("message-2", MessageHeaders.from("header" -> "value2"))
      )

      val publishSink = f.publisher.sinkWithHeaders[String](settings)
      val result      = Source(messages).runWith(publishSink)

      whenReady(result) { res =>
        res mustBe Done
        val expectedRequest1 = PublishRequest
          .builder()
          .topicArn(settings.topicArn)
          .message("message-1")
          .messageAttributes(
            Map("header" -> MessageAttributeValue.builder().dataType("String").stringValue("value1").build()).asJava
          )
          .build()

        verify(f.snsClient).publish(expectedRequest1)

        val expectedRequest2 = PublishRequest
          .builder()
          .topicArn(settings.topicArn)
          .message("message-2")
          .messageAttributes(
            Map("header" -> MessageAttributeValue.builder().dataType("String").stringValue("value2").build()).asJava
          )
          .build()

        verify(f.snsClient).publish(expectedRequest2)
        verifyNoMoreInteractions(f.snsClient)
      }
    }

    "publish messages from an ActorRef" in { f =>
      val settings = Settings("topic-arn")
      val actorRef = f.publisher.actor[String](settings)(Int.MaxValue, OverflowStrategy.dropHead)

      actorRef ! "message-1"
      expectNoMessage(100.millis)
      actorRef ! "message-2"
      expectNoMessage(100.millis)

      within(100.millis) {
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
        verifyNoMoreInteractions(f.snsClient)
      }
    }

    "restarts publisher sink in case of a failure" in { f =>
      val settings = Settings("topic-arn")
      val messages = "message-1" :: "message-2" :: Nil

      Mockito
        .doThrow(new RuntimeException("publish exception"))
        .when(f.snsClient)
        .publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())

      val result = f.publisher.publishAsync[String](settings)(messages: _*)

      whenReady(result) { res =>
        res mustBe Done
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build())
        verify(f.snsClient).publish(PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build())
        verifyNoMoreInteractions(f.snsClient)
      }
    }

  }

  case class FixtureParam(publisher: Publisher, snsClient: SnsAsyncClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val snsClient: SnsAsyncClient = mock[SnsAsyncClient](classOf[SnsAsyncClient])

    when(snsClient.publish(ArgumentMatchers.any[PublishRequest])).thenReturn {
      val response = PublishResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val publisher = Publisher()
    val fixture   = FixtureParam(publisher, snsClient)
    super.withFixture(test.toNoArgTest(fixture))
  }

}
