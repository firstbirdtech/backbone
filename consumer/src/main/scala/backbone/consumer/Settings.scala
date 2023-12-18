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

import org.apache.pekko.stream.connectors.sqs.SqsSourceSettings

import java.util.Optional
import scala.jdk.OptionConverters._

object Settings {

  /**
   * Java API
   */
  def create(queueUrl: String): Settings = {
    apply(queueUrl)
  }

  /**
   * Java API
   */
  def create(queueUrl: String, parallelism: Int): Settings = {
    apply(queueUrl, parallelism)
  }

  /**
   * Java API
   */
  def create(queueUrl: String, parallelism: Int, limitation: Optional[Limitation]): Settings = {
    apply(queueUrl, parallelism, limitation.toScala)
  }

  /**
   * Java API
   */
  def create(
      queueUrl: String,
      parallelism: Int,
      limitation: Optional[Limitation],
      receiveSettings: SqsSourceSettings): Settings = {
    apply(queueUrl, parallelism, limitation.toScala, receiveSettings)
  }

}

/**
 * Settings for the consumer
 *
 * @param queueUrl
 *   the url of a queue to consume from
 * @param parallelism
 *   number of concurrent messages in process
 * @param limitation
 *   optional limitation when consumer should stop consuming
 * @param receiveSettings
 *   settings for the SQS Source from alpakka
 */
final case class Settings(
    queueUrl: String,
    parallelism: Int = 1,
    limitation: Option[Limitation] = None,
    receiveSettings: SqsSourceSettings = SqsSourceSettings.Defaults
) {
  assert(queueUrl.trim.nonEmpty, "queueUrl must be not blank")
  assert(parallelism > 0, "parallelism must be positive")
}
