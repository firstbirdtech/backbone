package backbone.consumer

import java.util.{List => JList, Optional => JOption}

import scala.jdk.CollectionConverters._
import scala.compat.java8.OptionConverters

object ConsumerSettings {

  def create(
      topics: JList[String],
      queue: String,
      kmsKeyAlias: JOption[String],
      parallelism: Integer,
      consumeWithin: JOption[Limitation]
  ): ConsumerSettings = {
    apply(topics.asScala.toList,
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
      receiveSettings: ReceiveSettings
  ): ConsumerSettings =
    apply(topics.asScala.toList,
          queue,
          OptionConverters.toScala(kmsKeyAlias),
          parallelism,
          OptionConverters.toScala(consumeWithin),
          receiveSettings)

}

/**
 *
 * @param topics          a list of topics to subscribe to
 * @param queue           the name of a queue to consume from
 * @param kmsKeyAlias     optional kms key alias if queue should be encrypted
 * @param parallelism     number of concurrent messages in process
 * @param consumeWithin   optional limitation when backbone should stop consuming
 * @param receiveSettings settings for the SQS Source from alpakka
 *
 */
case class ConsumerSettings(
    topics: List[String],
    queue: String,
    kmsKeyAlias: Option[String] = None,
    parallelism: Int = 1,
    consumeWithin: Option[Limitation] = None,
    receiveSettings: ReceiveSettings = ReceiveSettings.Defaults
)
