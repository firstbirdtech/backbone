package backbone

sealed trait ProcessingResult
case object Rejected extends ProcessingResult
case object Consumed extends ProcessingResult {
  def instance = Consumed
}
