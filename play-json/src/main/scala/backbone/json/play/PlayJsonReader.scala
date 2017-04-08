package backbone.json.play

import backbone.consumer.Consumer
import backbone.consumer.Consumer.KeepMessage
import backbone.json.{JsonReader, SnsEnvelope}
import play.api.libs.json
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Left, Right}

class PlayJsonReader extends JsonReader {

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
