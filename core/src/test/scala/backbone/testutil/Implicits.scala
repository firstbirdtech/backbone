package backbone.testutil

import backbone.MessageReader
import backbone.json.{JsonReader, SnsEnvelope}
import io.circe.{Decoder, Encoder}

object Implicits {

  implicit val stringFormat = new MessageReader[String] {
    override def read(s: String): String = s
  }

  implicit val snsEnvelopeDecoder: Decoder[SnsEnvelope] =
    Decoder.forProduct1("Message")(SnsEnvelope)

  implicit val snsEnvelopeEncode: Encoder[SnsEnvelope] =
    Encoder.forProduct1("Message")(e => e.message)

  implicit val jsonSnsEnvelopReader: JsonReader = new TestJsonReader
}
