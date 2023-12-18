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

import org.apache.pekko.actor.ActorSystem
import backbone._
import backbone.consumer.scaladsl.Consumer
import backbone.consumer.{CountLimitation, Settings => CSettings}
import backbone.publisher.scaladsl.Publisher
import backbone.publisher.{Settings => PSettings}
import com.github.pjfanning.pekkohttpspi.PekkoHttpClient
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

/**
 * Uses backbone-consumer and backbone-publisher modules (all AWS resources managed by cloudformation)
 */
object BackboneConsumerPublisherDemoApp {

  private[this] implicit val system: ActorSystem  = ActorSystem()
  private[this] implicit val ec: ExecutionContext = system.dispatcher

  private[this] implicit val sqs: SqsAsyncClient = SqsAsyncClient
    .builder()
    .credentialsProvider(ProfileCredentialsProvider.create("backbone-demo"))
    .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
    .build()

  private[this] implicit val sns: SnsAsyncClient = SnsAsyncClient
    .builder()
    .credentialsProvider(ProfileCredentialsProvider.create("backbone-demo"))
    .httpClient(PekkoHttpClient.builder().withActorSystem(system).build())
    .build()

  def main(args: Array[String]): Unit = {
    val consumer  = Consumer()
    val publisher = Publisher()

    val queueUrl = "<stack output: BackboneConsumerPublisherQueue>"
    val topicArn = "<stack output: BackboneConsumerPublisherTopic>"

    val consumerSettings = CSettings(queueUrl, limitation = Some(CountLimitation(3)))

    val consumeResult = consumer.consume[String](consumerSettings) { msg =>
      println(s"Received: $msg")
      Consumed
    }

    val publisherSettings = PSettings(topicArn)

    val result = for {
      _ <- publisher.publishAsync[String](publisherSettings)("msg-1")
      _ <- publisher.publishAsync[String](publisherSettings)("msg-2")
      _ <- publisher.publishAsync[String](publisherSettings)("msg-3")
      _ <- consumeResult
      _ <- system.terminate()
    } yield ()

    Await.result(result, 5.seconds)
  }

}
