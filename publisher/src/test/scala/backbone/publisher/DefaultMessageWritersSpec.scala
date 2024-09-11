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
import backbone.testutil.BaseTest
import org.scalatest.flatspec.AnyFlatSpec

class DefaultMessageWritersSpec extends AnyFlatSpec with BaseTest with DefaultMessageWriters {

  "DefaultMessageWriters" should "read a String to a Short value" in {
    testMessageWriter[Short](1) mustBe "1"
  }

  it should "write a Int to a String value" in {
    testMessageWriter[Int](1) mustBe "1"
  }

  it should "write a Long to a String value" in {
    testMessageWriter[Long](1L) mustBe "1"
  }

  it should "write a Float to a String value" in {
    testMessageWriter[Float](1.12f) mustBe "1.12"
  }

  it should "write a Double to a String value" in {
    testMessageWriter[Double](1.12d) mustBe "1.12"
  }

  it should "write a Boolean to a String value" in {
    testMessageWriter[Boolean](true) mustBe "true"
  }

  it should "write a String to a String value" in {
    testMessageWriter[String]("message") mustBe "message"
  }

  it should "write an array of bytes to String value" in {
    testMessageWriter[Array[Byte]]("message".getBytes) mustBe "message"
  }

  it should "write an array of chars to String value" in {
    testMessageWriter[Array[Char]]("message".toCharArray) mustBe "message"
  }

  private[this] def testMessageWriter[T](message: T)(implicit fo: MessageWriter[T]): String = fo.write(message)

}
