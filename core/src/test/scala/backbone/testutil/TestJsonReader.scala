package backbone.testutil

import backbone.consumer.JsonReader
import io.circe.Decoder
import io.circe.parser.parse

class TestJsonReader extends JsonReader {

  private[this] implicit val snsEnvelopeDecoder: Decoder[JsonReader.SnsEnvelope] =
    Decoder.forProduct1("Message")(JsonReader.SnsEnvelope.apply)

  override def readSnsEnvelope(s: String): Option[JsonReader.SnsEnvelope] = {
    for {
      json     <- parse(s).toOption
      envelope <- json.as[JsonReader.SnsEnvelope].toOption
    } yield envelope
  }

}
