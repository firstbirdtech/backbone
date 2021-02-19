package backbone.testutil

import backbone.json.{JsonReader, SnsEnvelope}
import backbone.{MandatoryMessageReader, MessageReader}
import io.circe.{Decoder, Encoder}

import scala.util.Try

object Implicits {

  implicit val stringFormat: MessageReader[String] = MandatoryMessageReader(Try(_))

  implicit val snsEnvelopeDecoder: Decoder[SnsEnvelope] =
    Decoder.forProduct1("Message")(SnsEnvelope)

  implicit val snsEnvelopeEncode: Encoder[SnsEnvelope] =
    Encoder.forProduct1("Message")(e => e.message)

  implicit val jsonSnsEnvelopReader: JsonReader = new TestJsonReader
}
