/*
 * Copyright (c) 2021 Backbone contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
   * @param s String to be read as T
   * @return T read from the String.
   */
  def read(s: String): Try[Option[T]]
}

/**
 * This companion object contains scala apply methods which create MessageReaders
 */
object MessageReader {

  def apply[T](f: String => Try[Option[T]]): MessageReader[T] = (s: String) => f(s)
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
