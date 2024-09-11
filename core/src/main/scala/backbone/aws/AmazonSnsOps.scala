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

package backbone.aws

import backbone.scaladsl.Backbone.QueueInformation
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.SubscribeRequest

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}

private[backbone] trait AmazonSnsOps {

  def sns: SnsAsyncClient

  def subscribe(queue: QueueInformation, topicArn: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val request = SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queue.arn).build()
    sns.subscribe(request).toScala.map(_ => ())
  }

}
