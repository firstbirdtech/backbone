package backbone.gson

import backbone.json.SnsEnvelope
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class GsonJsonReaderSpec extends AnyFlatSpec with Matchers {

  private[this] val reader = new GsonJsonReader

  "GsonJsonReader " should "return Some(SnsEnvelope) if it can successfully decode a sns envelope json string" in {
    val json =
      """
        |{
        | "Message": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) must contain(SnsEnvelope("test-message"))
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
