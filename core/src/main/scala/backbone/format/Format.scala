package backbone.format

import scala.util.Try

trait Format[T] {
  def read(s: String): Try[T]
}
