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

package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[backbone] trait AmazonSqsOps {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  def sqs: SqsAsyncClient

  def getQueueArn(queueUrl: String)(implicit ec: ExecutionContext): Future[String] = {
    val request = GetQueueAttributesRequest
      .builder()
      .queueUrl(queueUrl)
      .attributeNames(QueueAttributeName.QUEUE_ARN)
      .build()

    sqs.getQueueAttributes(request).toScala.map { response =>
      val attributes = response.attributes.asScala
      attributes(QueueAttributeName.QUEUE_ARN)
    }
  }

  def savePolicy(queueUrl: String, policy: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val attributes = Map(QueueAttributeName.POLICY -> policy)
    val request    = SetQueueAttributesRequest.builder().queueUrl(queueUrl).attributes(attributes.asJava).build()

    sqs.setQueueAttributes(request).toScala.map(_ => ())
  }

  def createQueue(params: CreateQueueParams)(implicit ec: ExecutionContext): Future[QueueInformation] = {
    logger.info(s"Creating queue. queueName=$params.name")

    val attributes = params.kmsKeyId.map(key => QueueAttributeName.KMS_MASTER_KEY_ID -> key).toMap
    val request    = CreateQueueRequest.builder().queueName(params.name).attributes(attributes.asJava).build()

    for {
      createResponse <- sqs.createQueue(request).toScala
      url = createResponse.queueUrl()
      _   <- Future.successful(logger.debug(s"Created queue. queueParams=$params, url=$url"))
      arn <- getQueueArn(url)
      _   <- Future.successful(logger.debug(s"Requested queueArn. queueParams=$params, queueArn=$arn"))
    } yield QueueInformation(url, arn)
  }

}
