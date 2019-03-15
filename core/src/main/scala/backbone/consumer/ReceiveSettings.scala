package backbone.consumer

import akka.stream.alpakka.sqs.{AttributeName, MessageAttributeName}

object ReceiveSettings {
  val Defaults = ReceiveSettings(20, 100, 10, Nil, Nil)

  def create(
              waitTimeSeconds: Int,
              maxBufferSize: Int,
              maxBatchSize: Int,
              attributeNames: Seq[AttributeName] = Nil,
              messageAttributes: Seq[MessageAttributeName] = Nil
            ): ReceiveSettings =
    ReceiveSettings(waitTimeSeconds, maxBufferSize, maxBatchSize, attributeNames, messageAttributes)

}

case class ReceiveSettings(
                            waitTimeSeconds: Int,
                            maxBufferSize: Int,
                            maxBatchSize: Int,
                            attributeNames: Seq[AttributeName] = Nil,
                            messageAttributeNames: Seq[MessageAttributeName] = Nil
) {
  require(maxBatchSize <= maxBufferSize, "maxBatchSize must be lower or equal than maxBufferSize")
  // SQS requirements
  require(0 <= waitTimeSeconds && waitTimeSeconds <= 20,
          s"Invalid value ($waitTimeSeconds) for waitTimeSeconds. Requirement: 0 <= waitTimeSeconds <= 20 ")
  require(1 <= maxBatchSize && maxBatchSize <= 10,
          s"Invalid value ($maxBatchSize) for maxBatchSize. Requirement: 1 <= maxBatchSize <= 10 ")
}
