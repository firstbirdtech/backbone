package backbone.aws

import backbone.scaladsl.Backbone.SnsEnvelope
import play.api.libs.json.{Format, Reads, __}
import play.api.libs.functional.syntax._

object Implicits {

  implicit val snsEnvelopeReads: Format[SnsEnvelope] = (
    (__ \ 'Message).format[String] and
      (__ \ 'Subject).format[String]
  )(SnsEnvelope, unlift(SnsEnvelope.unapply))

}
