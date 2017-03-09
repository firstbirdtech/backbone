package backbone.javadsl

import java.util.{List => JList, Optional => JOption}

import backbone.consumer.Limitation

/**
 *
 * @param events        a java list of events to listen to
 * @param topics        a java list of topics to subscribe to
 * @param queue         the name of a queue to consume from
 * @param parallelism   number of concurrent messages in process
 * @param consumeWithin optional limitation when backbone should stop consuming
 */
case class ConsumerSettings(
    events: JList[String],
    topics: JList[String],
    queue: String,
    parallelism: Integer,
    consumeWithin: JOption[Limitation]
)
