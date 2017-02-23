package backbone.format

import scala.util.Try

/**
 * Typeclass to allow reading elements of type T from Amazon SQS Messages
 * @tparam T
 */
trait Format[T] {

  /**
   *
   * @param s String to be read as T
   * @return Success[T] if reading was successful. Failure[T] if reading failed.
   */
  def read(s: String): Try[T]
}
