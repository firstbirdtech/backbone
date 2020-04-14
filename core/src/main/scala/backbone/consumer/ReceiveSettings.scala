package backbone.consumer

import java.time.Duration
import java.{util => ju}

import akka.stream.alpakka.sqs.{MessageAttributeName, MessageSystemAttributeName}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object ReceiveSettings {
  val Defaults: ReceiveSettings = ReceiveSettings(
    waitTimeSeconds = 20,
    maxBufferSize = 100,
    maxBatchSize = 10,
    parallelRequests = 1,
    attributeNames = immutable.Seq.empty,
    messageAttributeNames = immutable.Seq.empty,
    closeOnEmptyReceive = false,
    visibilityTimeout = None
  )

  /**
   * Scala API
   */
  def apply(): ReceiveSettings = Defaults

  /**
   * Java API
   */
  def create(): ReceiveSettings = Defaults

}

case class ReceiveSettings(
    waitTimeSeconds: Int,
    maxBufferSize: Int,
    maxBatchSize: Int,
    parallelRequests: Int,
    attributeNames: immutable.Seq[MessageSystemAttributeName],
    messageAttributeNames: immutable.Seq[MessageAttributeName],
    closeOnEmptyReceive: Boolean,
    visibilityTimeout: Option[FiniteDuration]
) {
  require(maxBatchSize <= maxBufferSize, "maxBatchSize must be lower or equal than maxBufferSize")
  // SQS requirements
  require(0 <= waitTimeSeconds && waitTimeSeconds <= 20,
          s"Invalid value ($waitTimeSeconds) for waitTimeSeconds. Requirement: 0 <= waitTimeSeconds <= 20 ")
  require(1 <= maxBatchSize && maxBatchSize <= 10,
          s"Invalid value ($maxBatchSize) for maxBatchSize. Requirement: 1 <= maxBatchSize <= 10 ")

  def withWaitTime(value: FiniteDuration): ReceiveSettings = copy(waitTimeSeconds = value.toSeconds.toInt)

  /**
   * Java API
   */
  def withWaitTime(value: Duration): ReceiveSettings = withWaitTime(value.toMillis.millis)

  def withMaxBufferSize(value: Int): ReceiveSettings = copy(maxBufferSize = value)

  def withMaxBatchSize(value: Int): ReceiveSettings = copy(maxBatchSize = value)

  def withParallelRequests(value: Int): ReceiveSettings = copy(parallelRequests = value)

  def withAttribute(value: MessageSystemAttributeName): ReceiveSettings =
    copy(attributeNames = immutable.Seq(value))

  def withAttributes(value: immutable.Seq[MessageSystemAttributeName]): ReceiveSettings =
    copy(attributeNames = value)

  /**
   * Java API
   */
  def withAttributes(value: ju.List[MessageSystemAttributeName]): ReceiveSettings =
    copy(attributeNames = value.asScala.toList)

  def withMessageAttribute(value: MessageAttributeName): ReceiveSettings =
    copy(messageAttributeNames = immutable.Seq(value))

  def withMessageAttributes(value: immutable.Seq[MessageAttributeName]): ReceiveSettings =
    copy(messageAttributeNames = value)

  /**
   * Java API
   */
  def withMessageAttributes(value: ju.List[MessageAttributeName]): ReceiveSettings =
    copy(messageAttributeNames = value.asScala.toList)

  def withCloseOnEmptyReceive(value: Boolean): ReceiveSettings = copy(closeOnEmptyReceive = value)

  def withVisibilityTimeout(value: FiniteDuration): ReceiveSettings = copy(visibilityTimeout = Some(value))

  /**
   * Java API
   */
  def withVisibilityTimeout(value: Duration): ReceiveSettings = copy(visibilityTimeout = Some(value.toMillis.millis))

}
