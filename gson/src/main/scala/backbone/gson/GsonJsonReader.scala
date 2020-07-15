package backbone.gson
import backbone.json.{JsonReader, SnsEnvelope}
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * JsonReader using gson
 */
class GsonJsonReader extends JsonReader {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Option[SnsEnvelope] = {

    val maybeSnsEnvelope = for {
      json    <- Try(JsonParser.parseString(s)).map(_.getAsJsonObject).toOption
      message <- Option(json.get("Message")).map(_.getAsString)
    } yield SnsEnvelope(message)

    if (maybeSnsEnvelope.isEmpty) {
      logger.error(s"Unable to decode to SnsEnvelope. message=$s")
    }

    maybeSnsEnvelope
  }

}
