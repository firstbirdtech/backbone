package backbone.testutil

import backbone.json.{JsonReader, SnsEnvelope}
import backbone.testutil.Implicits._
import io.circe.parser.parse

class TestJsonReader extends JsonReader {

  override def readSnsEnvelope(s: String): Option[SnsEnvelope] = {
    for {
      json     <- parse(s).toOption
      envelope <- json.as[SnsEnvelope].toOption
    } yield envelope
  }
}
