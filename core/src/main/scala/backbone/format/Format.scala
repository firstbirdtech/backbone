package backbone.format

/**
 * Typeclass to allow reading elements of type T from Amazon SQS Messages
 * @tparam T
 */
trait Format[T] {

  /**
   *
   * @param s String to be read as T
   * @return T read from the String.
   */
  def read(s: String): T
}
