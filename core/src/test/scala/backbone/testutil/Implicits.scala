package backbone.testutil

import backbone.MessageReader
import backbone.json.{JsonReader, SnsEnvelope}
import io.circe.{Decoder, Encoder}

object Implicits {

  implicit val stringFormat = new MessageReader[String] {
    override def read(s: String): String = s
  }

  implicit val snsEnvelopeDecoder: Decoder[SnsEnvelope] =
    Decoder.forProduct2("Subject", "Message")(SnsEnvelope)

  implicit val snsEnvelopeEncode: Encoder[SnsEnvelope] =
    Encoder.forProduct2("Subject", "Message")(e => (e.subject, e.message))

  implicit val jsonSnsEnvelopReader: JsonReader = new TestJsonReader
}
