package backbone.playjson

import backbone.consumer.Consumer
import backbone.consumer.Consumer.KeepMessage
import backbone.json.{JsonReader, SnsEnvelope}
import org.slf4j.LoggerFactory
import play.api.libs.json
import play.api.libs.json._

import scala.util.{Left, Right}

class PlayJsonReader extends JsonReader {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] implicit val snsEnvelopeReads: json.Reads[SnsEnvelope] = (__ \ 'Message).read[String]
    .map(SnsEnvelope)

  override def readSnsEnvelope(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {
    Json.fromJson[SnsEnvelope](Json.parse(s)) match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) =>
        logger.error(s"Unable to decode to SnsEnvelope. message=$s, errors=$errors")
        Left(KeepMessage)
    }

  }
}
