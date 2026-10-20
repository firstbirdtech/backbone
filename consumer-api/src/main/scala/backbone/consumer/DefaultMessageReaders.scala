/*
 * Copyright (c) 2025 Backbone contributors
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

package backbone.consumer

import backbone.{MandatoryMessageReader, MessageReader}

import scala.util.{Success, Try}

/**
 * Default Message reads for primitive data types.
 */
trait DefaultMessageReaders {

  /**
   * Format to read a String value as a Short value.
   */
  implicit val shortFormat: MessageReader[Short] = MandatoryMessageReader(s => Try(s.toShort))

  /**
   * Format to read a String value as a Int value.
   */
  implicit val intFormat: MessageReader[Int] = MandatoryMessageReader(s => Try(s.toInt))

  /**
   * Format to read a String value as a Long value.
   */
  implicit val longFormat: MessageReader[Long] = MandatoryMessageReader(s => Try(s.toLong))

  /**
   * Format to read a String value as a Float value.
   */
  implicit val floatFormat: MessageReader[Float] = MandatoryMessageReader(s => Try(s.toFloat))

  /**
   * Format to read a String value as a Double value.
   */
  implicit val doubleFormat: MessageReader[Double] = MandatoryMessageReader(s => Try(s.toDouble))

  /**
   * Format to read a String value as a Boolean value.
   */
  implicit val booleanFormat: MessageReader[Boolean] = MandatoryMessageReader(s => Try(s.toBoolean))

  /**
   * Format to read a String value as a String value.
   */
  implicit val stringFormat: MessageReader[String] = MandatoryMessageReader(s => Success(s))

  /**
   * Format to read a String value as an array of Bytes.
   */
  implicit val byteFormat: MessageReader[Array[Byte]] = MandatoryMessageReader(s => Success(s.getBytes))

  /**
   * Format to read a String value as an array of Chars.
   */
  implicit val charFormat: MessageReader[Array[Char]] = MandatoryMessageReader(s => Success(s.toCharArray))

}

object DefaultMessageReaders extends DefaultMessageReaders
