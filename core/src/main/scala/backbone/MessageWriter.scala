package backbone

/**
 * Typeclass to write elements of type T to String to allow publishing to an Amazon SNS topic
 * @tparam T
 */
trait MessageWriter[T] {

  /**
   *
   * @param message message to be written as String
   * @return String write from the message
   */
  def write(message: T): String

}

object MessageWriter {
  def apply[T](f: T => String): MessageWriter[T] =
    new MessageWriter[T] {
      override def write(message: T): String = f(message)
    }
}
