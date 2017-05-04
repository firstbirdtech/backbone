package backbone

sealed trait ProcessingResult
case object Rejected extends ProcessingResult {
  def instance: ProcessingResult = Rejected
}
case object Consumed extends ProcessingResult {
  def instance: ProcessingResult = Consumed
}
