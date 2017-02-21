package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import com.amazonaws.auth.policy.Policy
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait AmazonSqsOps extends AmazonAsync {

  def sqs: AmazonSQSAsyncClient

  def getQueueArn(queueUrl: String)(implicit ec: ExecutionContext): Future[String] = {
    val attributeNames = List(QueueAttributeName.QueueArn.toString)

    for {
      response <- async[GetQueueAttributesRequest, GetQueueAttributesResult](
        sqs.getQueueAttributesAsync(queueUrl, attributeNames.asJava, _)
      )
      attributes = response.getAttributes.asScala
      arn        = attributes(QueueAttributeName.QueueArn.toString)
    } yield arn
  }

  def savePolicy(queueUrl: String, policy: Policy)(implicit ec: ExecutionContext): Future[Unit] = {
    val attributes = Map(QueueAttributeName.Policy.toString -> policy.toJson)
    async[SetQueueAttributesRequest, SetQueueAttributesResult](
      sqs.setQueueAttributesAsync(queueUrl, attributes.asJava, _)
    ).map(_ => ())
  }

  def createQueue(name: String)(implicit ec: ExecutionContext): Future[QueueInformation] = {
    for {
      createResponse <- async[CreateQueueRequest, CreateQueueResult](sqs.createQueueAsync(name, _))
      url = createResponse.getQueueUrl
      arn <- getQueueArn(url)
    } yield QueueInformation(url, arn)
  }

  def deleteMessage(queueUrl: String, receiptHandle: String)(implicit ec: ExecutionContext): Future[Unit] = {
    async[DeleteMessageRequest, DeleteMessageResult](sqs.deleteMessageAsync(queueUrl, receiptHandle, _)).map(_ => ())
  }

}
