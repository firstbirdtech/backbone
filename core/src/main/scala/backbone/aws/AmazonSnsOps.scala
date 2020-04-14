package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import scala.concurrent.{ExecutionContext, Future}
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.SubscribeRequest
import compat.java8.FutureConverters._

private[backbone] trait AmazonSnsOps {

  def sns: SnsAsyncClient

  def subscribe(queue: QueueInformation, topicArn: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val request = SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queue.arn).build()
    sns.subscribe(request).toScala.map(_ => ())
  }

}
