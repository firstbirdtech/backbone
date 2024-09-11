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

package backbone.scaladsl

import akka.Done
import akka.stream.scaladsl.Source
import backbone.publisher.{DefaultMessageWriters, PublisherSettings}
import backbone.testutil.{BaseTest, TestActorSystem}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model._
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import java.util.concurrent.CompletableFuture
import scala.concurrent.duration._

class BackbonePublishSpec extends FixtureAnyWordSpec with BaseTest with TestActorSystem with DefaultMessageWriters {

  "Backbone.publishAsync" should {

    "publish a single message to an SNS topic" in { f =>
      val settings = PublisherSettings("topic-arn")
      val result   = f.backbone.publishAsync("message", settings)

      whenReady(result) { res =>
        res mustBe Done

        val req = PublishRequest
          .builder()
          .topicArn(settings.topicArn)
          .message("message")
          .build()

        verify(f.snsClient).publish(req)
      }
    }

    "publish multiple messages to an SNS topic" in { f =>
      val messages = "message-1" :: "message-2" :: Nil
      val settings = PublisherSettings("topic-arn")
      val result   = f.backbone.publishAsync(messages, settings)

      whenReady(result) { res =>
        res mustBe Done

        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from an ActorRef" in { f =>
      val settings = PublisherSettings("topic-arn")
      val actorRef = f.backbone.actorPublisher[String](settings)

      actorRef ! "message-1"
      expectNoMessage(100.millis)
      actorRef ! "message-2"
      expectNoMessage(100.millis)

      within(100.millis) {
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

    "publish messages from a Sink" in { f =>
      val settings = PublisherSettings("topic-arn")
      val sink     = f.backbone.publisherSink[String](settings)

      val result = Source("message-1" :: "message-2" :: Nil).runWith(sink)

      whenReady(result) { res =>
        res mustBe Done

        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-1").build()
        )
        verify(f.snsClient).publish(
          PublishRequest.builder().topicArn(settings.topicArn).message("message-2").build()
        )
      }
    }

  }

  case class FixtureParam(backbone: Backbone, snsClient: SnsAsyncClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val sqsClient = mock(classOf[SqsAsyncClient])
    implicit val snsClient = mock(classOf[SnsAsyncClient])

    when(snsClient.publish(any(classOf[PublishRequest]))).thenReturn {
      val response = PublishResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val backbone = Backbone()
    val param    = FixtureParam(backbone, snsClient)

    super.withFixture(test.toNoArgTest(param))
  }

}
