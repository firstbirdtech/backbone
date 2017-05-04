package backbone.consumer

import java.util.{List => JList, Optional => JOption}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters

object ConsumerSettings {
  def apply(events: List[String], topics: List[String], queue: String, consumeWithin: Limitation): ConsumerSettings =
    apply(events, topics, queue, 1, Some(consumeWithin))

  def apply(events: List[String],
            topics: List[String],
            queue: String,
            parallelism: Int,
            consumeWithin: Limitation): ConsumerSettings =
    apply(events, topics, queue, parallelism, Some(consumeWithin))

  def create(events: JList[String],
             topics: JList[String],
             queue: String,
             parallelism: Integer,
             consumeWithin: JOption[Limitation]): ConsumerSettings = {
    apply(events.asScala.toList, topics.asScala.toList, queue, parallelism, OptionConverters.toScala(consumeWithin))
  }

}

/**
 *
 * @param events        a list of events to listen to
 * @param topics        a list of topics to subscribe to
 * @param queue         the name of a queue to consume from
 * @param parallelism   number of concurrent messages in process
 * @param consumeWithin optional limitation when backbone should stop consuming
 */
case class ConsumerSettings(
    events: List[String],
    topics: List[String],
    queue: String,
    parallelism: Int = 1,
    consumeWithin: Option[Limitation] = None
)
