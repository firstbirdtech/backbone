package backbone.format

trait Format[T] {
  def read(s: String): T
}
