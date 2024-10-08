/*
 * Copyright (c) 2024 Backbone contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package backbone.publisher.scaladsl

import akka.Done
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import backbone.publisher.{DefaultMessageWriters, MessageHeaders, Settings}
import backbone.testutil.{BaseTest, TestActorSystem}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{MessageAttributeValue, PublishRequest, PublishResponse}

import java.util.concurrent.CompletableFuture
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

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
    implicit val snsClient: SnsAsyncClient = mock(classOf[SnsAsyncClient])

    when(snsClient.publish(any(classOf[PublishRequest]))).thenReturn {
      val response = PublishResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val publisher = Publisher()
    val fixture   = FixtureParam(publisher, snsClient)
    super.withFixture(test.toNoArgTest(fixture))
  }

}
