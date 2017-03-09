package backbone.aws

import backbone.scaladsl.Backbone.SnsEnvelope
import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Format}

object Implicits {

  implicit val snsEnvelopeReads: Format[SnsEnvelope] = (
    (__ \ 'Message).format[String] and
      (__ \ 'Subject).format[String]
  )(SnsEnvelope, unlift(SnsEnvelope.unapply))

}
