package backbone.testutil

import backbone.consumer.Consumer
import backbone.consumer.Consumer.KeepMessage
import backbone.json.{SnsEnvelope, JsonReader}
import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json.{__, JsError, JsSuccess, Json}

import scala.util.{Left, Right}

class TestJsonReader extends JsonReader {

  private implicit val snsEnvelopeReads: json.Format[SnsEnvelope] = (
    (__ \ 'Message).format[String] and
      (__ \ 'Subject).format[String]
  )(SnsEnvelope, unlift(SnsEnvelope.unapply))

  override def read(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {
    Json.fromJson[SnsEnvelope](Json.parse(s)) match {
      case JsSuccess(value, _) => Right(value)
      case JsError(_)          => Left(KeepMessage)
    }

  }
}
