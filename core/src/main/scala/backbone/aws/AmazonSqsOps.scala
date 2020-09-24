package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import cats.syntax.all._
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
      _   <- logger.debug(s"Created queue. queueParams=$params, url=$url").pure[Future]
      arn <- getQueueArn(url)
      _   <- logger.debug(s"Requested queueArn. queueParams=$params, queueArn=$arn").pure[Future]
    } yield QueueInformation(url, arn)
  }

}
