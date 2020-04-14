package backbone.gson

import backbone.consumer.Consumer.KeepMessage
import backbone.json.SnsEnvelope
import cats.syntax.either._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class GsonJsonReaderSpec extends AnyFlatSpec with Matchers {

  private[this] val reader = new GsonJsonReader

  "GsonJsonReader " should "return Right(SnsEnvelope) if it can successfully decode a sns envelope json string" in {
    val json =
      """
        |{
        | "Message": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) mustBe Right(SnsEnvelope("test-message"))
  }

  it should "return Left(KeepMessage) if it can not parse a sns envelope json string" in {
    val json =
      """
        |{
        | "NotAMessage": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) mustBe Left(KeepMessage)
  }

  it should "return Left(KeepMessage) if the sns envelope is not formatted properly" in {
    val json = "[1,2,3]"
    reader.readSnsEnvelope(json) mustBe KeepMessage.asLeft
  }

}
