package backbone.playjson

import backbone.consumer.Consumer
import backbone.consumer.Consumer.KeepMessage
import backbone.json.{JsonReader, SnsEnvelope}
import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json._

import scala.util.{Left, Right}

class PlayJsonReader extends JsonReader {

  private[this] implicit val snsEnvelopeReads: json.Format[SnsEnvelope] = (
    (__ \ 'Subject).format[String] and
      (__ \ 'Message).format[String]
  )(SnsEnvelope, unlift(SnsEnvelope.unapply))

  override def readSnsEnvelope(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {
    Json.fromJson[SnsEnvelope](Json.parse(s)) match {
      case JsSuccess(value, _) => Right(value)
      case JsError(_)          => Left(KeepMessage)
    }

  }
}
