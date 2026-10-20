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

import akka.stream.alpakka.sqs.{MessageSystemAttributeName, SqsSourceSettings}
import backbone.Consumed
import backbone.consumer.{ConsumerSettings, CountLimitation, JsonReader}
import backbone.testutil.Helpers._
import backbone.testutil._
import cats.syntax.all._
import io.circe.syntax._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.Outcome
import org.scalatest.wordspec.FixtureAnyWordSpec
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{SubscribeRequest, SubscribeResponse}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import java.util.concurrent.CompletableFuture
import scala.annotation.nowarn
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

@nowarn
class BackboneSubscriptionSpec extends FixtureAnyWordSpec with BaseTest with TestActorSystem {

  "Backbone.consume" should {

    "subscribe the queue with it's arn to the provided topics" in { f =>
      val settings =
        ConsumerSettings("topic-arn" :: "topic-arn-2" :: Nil, "Queue-name", None, 1, Some(CountLimitation(0)))

      val result = f.backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        val request1 = SubscribeRequest
          .builder()
          .topicArn("topic-arn")
          .protocol("sqs")
          .endpoint("queue-arn")
          .build()

        val request2 = SubscribeRequest
          .builder()
          .topicArn("topic-arn-2")
          .protocol("sqs")
          .endpoint("queue-arn")
          .build()

        verify(f.snsClient).subscribe(request1)
        verify(f.snsClient).subscribe(request2)
      }
    }

    "create a queue with the configured name" in { f =>
      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(0)))

      val result = f.backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        verify(f.sqsClient)
          .createQueue(CreateQueueRequest.builder().queueName("queue-name").attributes(Map().asJava).build())
      }
    }

    "create an encrypted queue with the configured name and kms key alias" in { f =>
      val settings = ConsumerSettings(
        Nil,
        "queue-name",
        "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some,
        1,
        Some(CountLimitation(0))
      )

      val result = f.backbone.consume[String](settings)(_ => Consumed)

      whenReady(result) { _ =>
        val request = CreateQueueRequest
          .builder()
          .queueName("queue-name")
          .attributes(
            Map(QueueAttributeName.KMS_MASTER_KEY_ID -> "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias").asJava
          )
          .build()

        verify(f.sqsClient).createQueue(request)
      }
    }

    "request messages form the queue url returned when creating the queue" in { f =>
      val envelope = JsonReader.SnsEnvelope("message")
      val message  = Message.builder().body(envelope.asJson.toString()).build()
      when(f.sqsClient.receiveMessage(any(classOf[ReceiveMessageRequest]))).thenReturn {
        val result = ReceiveMessageResponse.builder().messages(List(message).asJava).build()
        CompletableFuture.completedFuture(result)
      }

      val receiveSettings = SqsSourceSettings.Defaults
        .withAttribute(MessageSystemAttributeName.senderId)
        .withVisibilityTimeout(1.minute)

      val settings =
        ConsumerSettings("subject" :: Nil, "queue-name", None, 1, Some(CountLimitation(1)), receiveSettings)

      f.backbone.consume[String](settings)(_ => Consumed).futureValue

      val captor = ArgumentCaptor.forClass(classOf[ReceiveMessageRequest])

      verify(f.sqsClient, Mockito.atLeastOnce()).receiveMessage(captor.capture())
      val request = captor.getValue()

      request.maxNumberOfMessages() mustBe 10
      request.waitTimeSeconds() mustBe 20
      request.attributeNamesAsStrings() mustBe List("SenderId").asJava
      request.messageAttributeNames() mustBe List("All").asJava
      request.visibilityTimeout() mustBe 60
      request.queueUrl() mustBe "queue-url"
    }
  }

  case class FixtureParam(backbone: Backbone, sqsClient: SqsAsyncClient, snsClient: SnsAsyncClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    implicit val sqsClient = mock(classOf[SqsAsyncClient])

    when(sqsClient.createQueue(any(classOf[CreateQueueRequest]))).thenReturn {
      val result = CreateQueueResponse.builder().queueUrl("queue-url").build()
      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.getQueueAttributes(any[GetQueueAttributesRequest])).thenReturn {
      val result = GetQueueAttributesResponse
        .builder()
        .attributes(Map(QueueAttributeName.QUEUE_ARN -> "queue-arn").asJava)
        .build()

      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.setQueueAttributes(any[SetQueueAttributesRequest])).thenReturn {
      val result = SetQueueAttributesResponse.builder().build()
      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.deleteMessage(any[DeleteMessageRequest])).thenReturn {
      val result = DeleteMessageResponse.builder().build()
      CompletableFuture.completedFuture(result)
    }

    implicit val snsClient = mock(classOf[SnsAsyncClient])

    when(snsClient.subscribe(any(classOf[SubscribeRequest]))).thenReturn {
      val response = SubscribeResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    val backbone = Backbone()

    val fixture = FixtureParam(backbone, sqsClient, snsClient)
    super.withFixture(test.toNoArgTest(fixture))
  }

}
