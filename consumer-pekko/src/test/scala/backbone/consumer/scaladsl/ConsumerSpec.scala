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

package backbone.consumer.scaladsl

import backbone.consumer.{CountLimitation, JsonReader, Settings, _}
import backbone.testutil.Helpers._
import backbone.testutil.{BaseTest, ElasticMQ, TestActorSystem}
import backbone.{Consumed, MessageReader, ProcessingResult, Rejected}
import io.circe.syntax._
import org.mockito.Mockito._
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

    "consume messages from the queue url inlcuding message headers" in {
      val queueName = "test-queue"
      val queueUrl  = s"$elasticMqHost/000000000000/$queueName"

      val settings = Settings(queueUrl, limitation = Some(CountLimitation(1)))
      val consumer = Consumer(testJsonReader)

      val msg    = JsonReader.SnsEnvelope("message")
      val spy    = mock(classOf[(String, MessageHeaders) => Future[ProcessingResult]])
      val result = for {
        _ <- createQueue(queueName)
        _ <- sendMessage(msg.asJson.toString, queueUrl, "header" -> "value")
        _ <- consumer.consumeWithHeadersAsync[String](settings)(spy)
        r <- receiveMessage(queueUrl)
      } yield r

      whenReady(result) { res =>
        verify(spy).apply("message", MessageHeaders(Map("header" -> "value")))
      }
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
