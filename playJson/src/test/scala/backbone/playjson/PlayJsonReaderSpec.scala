package backbone.playjson

import backbone.consumer.Consumer.KeepMessage
import backbone.json.SnsEnvelope
import org.scalatest.{FlatSpec, MustMatchers}
import cats.syntax.either._

class PlayJsonReaderSpec extends FlatSpec with MustMatchers {
  private val reader = new PlayJsonReader()

  "PlayJsonReader " should "return Right(SnsEnvelope) if it can successfully decode a sns envelope json string" in {
    val json =
      """
        |{
        | "Subject": "test-subject",
        | "Message": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) mustBe SnsEnvelope("test-subject", "test-message").asRight
  }

  it should "return Left(KeepMessage) if it can not parse a sns envelope json string" in {
    val json =
      """
        |{
        | "xxx": "test-subject",
        | "Message": "test-message"
        |}
      """.stripMargin

    reader.readSnsEnvelope(json) mustBe KeepMessage.asLeft
  }

  it should "return Left(KeepMessage) if the sns envelope is not formatted properly" in {
    val json = "[1,2,3]"
    reader.readSnsEnvelope(json) mustBe KeepMessage.asLeft
  }

}
