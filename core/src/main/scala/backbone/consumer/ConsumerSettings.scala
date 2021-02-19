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

package backbone.consumer

import akka.stream.alpakka.sqs.SqsSourceSettings

import java.util.{List => JList, Optional => JOption}
import scala.compat.java8.OptionConverters
import scala.jdk.CollectionConverters._

object ConsumerSettings {

  def create(
      topics: JList[String],
      queue: String,
      kmsKeyAlias: JOption[String],
      parallelism: Integer,
      consumeWithin: JOption[Limitation]
  ): ConsumerSettings = {
    apply(
      topics.asScala.toList,
      queue,
      OptionConverters.toScala(kmsKeyAlias),
      parallelism,
      OptionConverters.toScala(consumeWithin))

  }

  def create(
      topics: JList[String],
      queue: String,
      kmsKeyAlias: JOption[String],
      parallelism: Integer,
      consumeWithin: JOption[Limitation],
      receiveSettings: SqsSourceSettings
  ): ConsumerSettings =
    apply(
      topics.asScala.toList,
      queue,
      OptionConverters.toScala(kmsKeyAlias),
      parallelism,
      OptionConverters.toScala(consumeWithin),
      receiveSettings)

}

/**
 * @param topics          a list of topics to subscribe to
 * @param queue           the name of a queue to consume from
 * @param kmsKeyAlias     optional kms key alias if queue should be encrypted
 * @param parallelism     number of concurrent messages in process
 * @param consumeWithin   optional limitation when backbone should stop consuming
 * @param receiveSettings settings for the SQS Source from alpakka
 */
case class ConsumerSettings(
    topics: List[String],
    queue: String,
    kmsKeyAlias: Option[String] = None,
    parallelism: Int = 1,
    consumeWithin: Option[Limitation] = None,
    receiveSettings: SqsSourceSettings = SqsSourceSettings.Defaults
)
