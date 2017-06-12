package backbone

import java.util.function.{Function => JFunction}
import java.util.{Optional => JOption}

import scala.compat.java8.OptionConverters._
import scala.util.Try

/**
 * Typeclass to allow reading elements of type T from Amazon SQS Messages
 *
 * @tparam T
 */
trait MessageReader[T] {

  /**
   *
   * @param s String to be read as T
   * @return T read from the String.
   */
  def read(s: String): Try[Option[T]]
}

object MessageReader {

  def apply[T](f: String => Try[Option[T]]): MessageReader[T] = new MessageReader[T] {
    override def read(s: String): Try[Option[T]] = f(s)
  }

  def create[T](f: JFunction[String, JOption[T]]): MessageReader[T] = MessageReader(s => Try(f.apply(s).asScala))
}

object MandatoryMessageReader {
  def apply[T](f: String => Try[T]): MessageReader[T]      = MessageReader(s => f(s).map(Option(_)))
  def create[T](f: JFunction[String, T]): MessageReader[T] = MessageReader(s => Try(f.apply(s)).map(Option(_)))
}
