package backbone.consumer

object SourceSettingsSqs {
  val Defaults = SourceSettingsSqs(20, 100, 10)

  def create(waitTimeSeconds: Int, maxBufferSize: Int, maxBatchSize: Int): SourceSettingsSqs =
    SourceSettingsSqs(waitTimeSeconds, maxBufferSize, maxBatchSize)

}

case class SourceSettingsSqs(
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

