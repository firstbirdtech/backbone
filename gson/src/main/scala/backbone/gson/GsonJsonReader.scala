package backbone.gson
import backbone.consumer.Consumer
import backbone.consumer.Consumer.KeepMessage
import backbone.json.{JsonReader, SnsEnvelope}
import cats.syntax.either._
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * JsonReader using gson
 */
class GsonJsonReader extends JsonReader {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {

    val optionalSnsEnvelope = for {
      json    <- Try(JsonParser.parseString(s)).map(_.getAsJsonObject).toOption
      message <- Option(json.get("Message")).map(_.getAsString)
    } yield SnsEnvelope(message)

    optionalSnsEnvelope match {
      case Some(envelope) => envelope.asRight
      case None =>
        logger.error(s"Unable to decode to SnsEnvelope. message=$s")
        KeepMessage.asLeft
    }
  }

}
