package backbone.consumer

import java.util.{List => JList, Optional => JOption}

import akka.stream.alpakka.sqs.SqsSourceSettings

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

  def apply(events: List[String],
            topics: List[String],
            queue: String,
            parallelism: Int,
            consumeWithin: Limitation,
            sqsSourceWaitTime: Int,
            maxBufferSize: Int,
            maxBatchSize: Int): ConsumerSettings =
    apply(events, topics, queue, parallelism, Some(consumeWithin), sqsSourceWaitTime, maxBufferSize, maxBatchSize)

  def create(events: JList[String],
             topics: JList[String],
             queue: String,
             parallelism: Integer,
             consumeWithin: JOption[Limitation]): ConsumerSettings = {
    apply(events.asScala.toList, topics.asScala.toList, queue, parallelism, OptionConverters.toScala(consumeWithin))

  }
  def create(events: JList[String],
             topics: JList[String],
             queue: String,
             parallelism: Integer,
             consumeWithin: JOption[Limitation],
             sqsSourceWaitTime: Integer,
             maxBufferSize: Integer,
             maxBatchSize: Integer): ConsumerSettings =
    apply(events.asScala.toList,
          topics.asScala.toList,
          queue,
          parallelism,
          OptionConverters.toScala(consumeWithin),
          sqsSourceWaitTime,
          maxBufferSize,
          maxBatchSize)

}

/**
 *
 * @param events            a list of events to listen to
 * @param topics            a list of topics to subscribe to
 * @param queue             the name of a queue to consume from
 * @param parallelism       number of concurrent messages in process
 * @param consumeWithin     optional limitation when backbone should stop consuming
 * @param sqsSourceWaitTime how long should the long polling request wait
 * @param maxBufferSize     maximum buffer of the SQS source
 * @param maxBatchSize      maximum messages received at one time from SQS
 *
 */
case class ConsumerSettings(
    events: List[String],
    topics: List[String],
    queue: String,
    parallelism: Int = 1,
    consumeWithin: Option[Limitation] = None,
    sqsSourceWaitTime: Int = 20,
    maxBufferSize: Int = 100,
    maxBatchSize: Int = 10
)
