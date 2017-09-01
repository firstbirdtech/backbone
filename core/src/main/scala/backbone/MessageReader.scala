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

/**
 * This companion object contains scala apply methods which create MessageReaders
 */
object MessageReader {

  def apply[T](f: String => Try[Option[T]]): MessageReader[T] = new MessageReader[T] {
    override def read(s: String): Try[Option[T]] = f(s)
  }
}

/**
 * This is an alternative object containing the same factory methods as MessageReader companion object.
 * This is needed for Java interop
 */
object OptionalMessageReader {
  def apply[T](f: String => Try[Option[T]]): MessageReader[T]       = MessageReader(f)
  def create[T](f: JFunction[String, JOption[T]]): MessageReader[T] = MessageReader(s => Try(f.apply(s).asScala))
}

/**
 * This object contains factory methods creating MessageReader instances that assume that in every case a
 * parsed message is returned or the parsing fails.
 */
object MandatoryMessageReader {
  def apply[T](f: String => Try[T]): MessageReader[T]      = MessageReader(s => f(s).map(Option(_)))
  def create[T](f: JFunction[String, T]): MessageReader[T] = MessageReader(s => Try(f.apply(s)).map(Option(_)))
}
