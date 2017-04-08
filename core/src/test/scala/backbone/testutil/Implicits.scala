package backbone.testutil

import backbone.MessageReader
import backbone.json.{JsonReader, SnsEnvelope}
import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json.__

object Implicits {

  implicit val stringFormat = new MessageReader[String] {
    override def read(s: String): String = s
  }

  implicit val snsEnvelopeReads: json.Format[SnsEnvelope] = (
    (__ \ 'Message).format[String] and
      (__ \ 'Subject).format[String]
  )(SnsEnvelope, unlift(SnsEnvelope.unapply))

  implicit val jsonSnsEnvelopReader: JsonReader = new TestJsonReader
}
