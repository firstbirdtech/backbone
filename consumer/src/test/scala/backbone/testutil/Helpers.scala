package backbone.testutil

import backbone.consumer.JsonReader
import io.circe.Encoder

object Helpers {

  implicit val snsEnvelopeEncoder: Encoder[JsonReader.SnsEnvelope] =
    Encoder.forProduct1("Message")(e => e.message)

  val testJsonReader: JsonReader = new TestJsonReader
}
