/*
 * Copyright (c) 2021 Backbone contributors
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

package backbone.it

import akka.actor.ActorSystem
import backbone._
import backbone.consumer.{ConsumerSettings, CountLimitation}
import backbone.publisher.PublisherSettings
import backbone.scaladsl.Backbone
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

/**
 * Uses the backbone-core module, which automatically creates and subscribes queue to an SNS topic
 */
object BackboneCoreDemoApp {

  private[this] implicit val system: ActorSystem  = ActorSystem()
  private[this] implicit val ec: ExecutionContext = system.dispatcher

  private[this] implicit val sqs: SqsAsyncClient = SqsAsyncClient
    .builder()
    .credentialsProvider(ProfileCredentialsProvider.create("backbone-demo"))
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()

  private[this] implicit val sns: SnsAsyncClient = SnsAsyncClient
    .builder()
    .credentialsProvider(ProfileCredentialsProvider.create("backbone-demo"))
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()

  def main(args: Array[String]): Unit = {
    val backbone = Backbone()

    val topicArn  = "<stack output: BackboneCoreTopic>"
    val queueName = "<queue name from stack output: BackboneCoreTopic>"

    val consumerSettings = ConsumerSettings(
      topicArn :: Nil,
      queueName,
      consumeWithin = Some(CountLimitation(3))
    )

    val consumeResult = backbone.consume[String](consumerSettings) { msg =>
      println(s"Received: $msg")
      Consumed
    }

    val publisherSettings = PublisherSettings(topicArn)

    val result = for {
      _ <- backbone.publishAsync[String]("msg-1", publisherSettings)
      _ <- backbone.publishAsync[String]("msg-2", publisherSettings)
      _ <- backbone.publishAsync[String]("msg-3", publisherSettings)
      _ <- consumeResult
      _ <- system.terminate()
    } yield ()

    Await.result(result, 10.seconds)
  }

}
