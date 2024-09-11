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

package backbone.consumer

import backbone.MessageReader
import backbone.testutil.BaseTest
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.{Success, Try}

class DefaultMessageReadersSpec extends AnyFlatSpec with BaseTest with DefaultMessageReaders {

  "DefaultMessageReaders" should "read a String to a Short value" in {
    testMessageReader[Short]("1").success.value mustBe Some(1)
  }

  it should "read a String to a Int value" in {
    testMessageReader[Int]("1") mustBe Success(Some(1))
  }

  it should "read a String to a Long value" in {
    testMessageReader[Long]("1") mustBe Success(Some(1L))
  }

  it should "read a String to a Float value" in {
    testMessageReader[Float]("1.12") mustBe Success(Some(1.12f))
  }

  it should "read a String to a Double value" in {
    testMessageReader[Double]("1.12") mustBe Success(Some(1.12d))
  }

  it should "read a String to a Boolean value" in {
    testMessageReader[Boolean]("true") mustBe Success(Some(true))
  }

  it should "read a String to a String value" in {
    testMessageReader[String]("message").success.value mustBe Some("message")
  }

  it should "read a String to an array of Bytes" in {
    val actual = testMessageReader[Array[Byte]]("message").success.value
    actual must contain("message".getBytes)
  }

  it must "read a String to an array of Chars" in {
    testMessageReader[Array[Char]]("message").success.value must contain("message".toCharArray)
  }

  private[this] def testMessageReader[T](message: String)(implicit fo: MessageReader[T]): Try[Option[T]] =
    fo.read(message)

}
