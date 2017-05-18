package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}

import scala.concurrent.{ExecutionContext, Future}

private[backbone] trait AmazonSnsOps extends AmazonAsync {

  def sns: AmazonSNSAsync

  def subscribe(queue: QueueInformation, topicArn: String)(implicit ec: ExecutionContext): Future[Unit] = {
    async[SubscribeRequest, SubscribeResult](sns.subscribeAsync(topicArn, "sqs", queue.arn, _)).map(_ => ())
  }

}
