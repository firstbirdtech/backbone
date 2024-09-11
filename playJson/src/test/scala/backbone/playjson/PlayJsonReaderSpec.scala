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

package backbone.playjson

import backbone.consumer.JsonReader
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PlayJsonReaderSpec extends AnyFlatSpec with Matchers {
  private[this] val reader = new PlayJsonReader()

  "PlayJsonReader " should "return Some(SnsEnvelope) if it can successfully decode a sns envelope json string" in {
    val json =
      """
        |{
        | "Message": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) must contain(JsonReader.SnsEnvelope("test-message"))
  }

  it should "return None if it can not parse a sns envelope json string" in {
    val json =
      """
        |{
        | "NotAMessage": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) mustBe empty
  }

  it should "return None if the sns envelope is not formatted properly" in {
    val json = "[1,2,3]"
    reader.readSnsEnvelope(json) mustBe empty
  }

}
