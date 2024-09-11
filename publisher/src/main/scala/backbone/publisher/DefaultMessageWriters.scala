/*
 * Copyright (c) 2024 Backbone contributors
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

package backbone.publisher

import backbone.MessageWriter

/**
 * Default message writes for primitive data types.
 */
trait DefaultMessageWriters {

  /**
   * Write a Short value to a String value.
   */
  implicit val shortWrite: MessageWriter[Short] = MessageWriter[Short](String.valueOf)

  /**
   * Write a Int value to a String value.
   */
  implicit val intWrite: MessageWriter[Int] = MessageWriter[Int](String.valueOf)

  /**
   * Write a Long value to a String value.
   */
  implicit val longWrite: MessageWriter[Long] = MessageWriter[Long](String.valueOf)

  /**
   * Write a Float value to a String value.
   */
  implicit val floatWrite: MessageWriter[Float] = MessageWriter[Float](String.valueOf)

  /**
   * Write a Double value to a String value.
   */
  implicit val doubleWrite: MessageWriter[Double] = MessageWriter[Double](String.valueOf)

  /**
   * Write a Boolean value to a String value.
   */
  implicit val booleanWrite: MessageWriter[Boolean] = MessageWriter[Boolean](String.valueOf)

  /**
   * Write a String value to a String value.
   */
  implicit val stringWrite: MessageWriter[String] = MessageWriter[String](identity)

  /**
   * Write a Array[Byte] value to a String value.
   */
  implicit val byteWrite: MessageWriter[Array[Byte]] = MessageWriter[Array[Byte]](new String(_))

  /**
   * Write a Array[Char] value to a String value.
   */
  implicit val charWrite: MessageWriter[Array[Char]] = MessageWriter[Array[Char]](new String(_))

}

object DefaultMessageWriters extends DefaultMessageWriters
