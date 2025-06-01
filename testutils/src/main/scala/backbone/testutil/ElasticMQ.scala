/*
 * Copyright (c) 2025 Backbone contributors
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

package backbone.testutil

import com.github.matsluni.akkahttpspi.AkkaHttpClient
import org.elasticmq.rest.sqs.{SQSRestServer, SQSRestServerBuilder}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, ListQueuesRequest, SendMessageRequest, _}

import java.net.URI
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

trait ElasticMQ extends BeforeAndAfterEach with BeforeAndAfterAll {
  this: TestSuite with TestActorSystem with ScalaFutures =>

  protected lazy val server: SQSRestServer = {
    SQSRestServerBuilder.withDynamicPort().start()
  }

  protected lazy val elasticMqHost: String = {
    val address = server.waitUntilStarted().localAddress
    s"http://${address.getHostName}:${address.getPort}"
  }

  protected implicit lazy val sqsClient: SqsAsyncClient = SqsAsyncClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
    .region(Region.EU_CENTRAL_1)
    .endpointOverride(URI.create(elasticMqHost))
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()

  protected def createQueue(
      name: String = "queue-name",
      attributes: Map[QueueAttributeName, String] = Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "1")): Future[Unit] = {
    val request = CreateQueueRequest
      .builder()
      .queueName(name)
      .attributes(attributes.asJava)
      .build()

    sqsClient
      .createQueue(request)
      .asScala
      .map(_ => ())
  }

  protected def sendMessage(message: String, queueUrl: String, headers: (String, String)*): Future[Unit] = {
    val sqsMessage = Message.builder().body(message).build()
    val request    = SendMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageAttributes(
        headers
          .map { case (k, v) => k -> MessageAttributeValue.builder().stringValue(v).dataType("String").build() }
          .toMap
          .asJava)
      .messageBody(sqsMessage.body)
      .build()
    sqsClient.sendMessage(request).asScala.map(_ => ())
  }

  protected def receiveMessage(queueUrl: String): Future[ReceiveMessageResponse] = {
    val receiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .messageAttributeNames(".*")
      .build()

    sqsClient
      .receiveMessage(receiveMessageRequest)
      .asScala
  }

  abstract override protected def afterAll(): Unit = {
    try super.afterAll()
    finally server.stopAndWait()
  }

  abstract override protected def beforeEach(): Unit = {
    try super.beforeEach()
    finally {
      val result = for {
        listResponse <- sqsClient.listQueues(ListQueuesRequest.builder().build()).asScala
        _            <- Future.sequence(
          listResponse.queueUrls().asScala.map(url => sqsClient.deleteQueue(_.queueUrl(url)).asScala)
        )
      } yield ()

      result.futureValue
    }
  }

}
