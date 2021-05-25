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

package backbone.format

import backbone.MessageWriter

/**
 * Default message writes for primitive data types.
 */
trait DefaultMessageWrites {

  /**
   * Write a Short value to a String value.
   */
  implicit val shortWrite: MessageWriter[Short] = new MessageWriter[Short] {
    override def write(message: Short): String = String.valueOf(message)
  }

  /**
   * Write a Int value to a String value.
   */
  implicit val intWrite: MessageWriter[Int] = new MessageWriter[Int] {
    override def write(message: Int): String = String.valueOf(message)
  }

  /**
   * Write a Long value to a String value.
   */
  implicit val longWrite: MessageWriter[Long] = new MessageWriter[Long] {
    override def write(message: Long): String = String.valueOf(message)
  }

  /**
   * Write a Float value to a String value.
   */
  implicit val floatWrite: MessageWriter[Float] = new MessageWriter[Float] {
    override def write(message: Float): String = String.valueOf(message)
  }

  /**
   * Write a Double value to a String value.
   */
  implicit val doubleWrite: MessageWriter[Double] = new MessageWriter[Double] {
    override def write(message: Double): String = String.valueOf(message)
  }

  /**
   * Write a Boolean value to a String value.
   */
  implicit val booleanWrite: MessageWriter[Boolean] = new MessageWriter[Boolean] {
    override def write(message: Boolean): String = String.valueOf(message)
  }

  /**
   * Write a String value to a String value.
   */
  implicit val stringWrite: MessageWriter[String] = new MessageWriter[String] {
    override def write(message: String): String = message
  }

  /**
   * Write a Array[Byte] value to a String value.
   */
  implicit val byteWrite: MessageWriter[Array[Byte]] = new MessageWriter[Array[Byte]] {
    override def write(message: Array[Byte]): String = new String(message)
  }

  /**
   * Write a Array[Char] value to a String value.
   */
  implicit val charWrite: MessageWriter[Array[Char]] = new MessageWriter[Array[Char]] {
    override def write(message: Array[Char]): String = new String(message)
  }

}
