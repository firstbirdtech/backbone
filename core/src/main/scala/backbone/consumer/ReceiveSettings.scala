package backbone.consumer

object ReceiveSettings {
  val Defaults = ReceiveSettings(20, 100, 10)

  def create(waitTimeSeconds: Int, maxBufferSize: Int, maxBatchSize: Int): ReceiveSettings =
    ReceiveSettings(waitTimeSeconds, maxBufferSize, maxBatchSize)

}

case class ReceiveSettings(
  waitTimeSeconds: Int,
  maxBufferSize: Int,
  maxBatchSize: Int
) {
  require(maxBatchSize <= maxBufferSize, "maxBatchSize must be lower or equal than maxBufferSize")
  // SQS requirements
  require(0 <= waitTimeSeconds && waitTimeSeconds <= 20,
    s"Invalid value ($waitTimeSeconds) for waitTimeSeconds. Requirement: 0 <= waitTimeSeconds <= 20 ")
  require(1 <= maxBatchSize && maxBatchSize <= 10,
    s"Invalid value ($maxBatchSize) for maxBatchSize. Requirement: 1 <= maxBatchSize <= 10 ")
}

