package backbone.aws

import backbone.scaladsl.Backbone.SnsEnvelope
import play.api.libs.json.{Reads, __}
import play.api.libs.functional.syntax._

object Implicits {

  implicit val snsEnvelopeReads: Reads[SnsEnvelope] = (
      (__ \ 'Message).read[String] and
          (__ \ 'Subject).read[String]
      )(SnsEnvelope)

}
