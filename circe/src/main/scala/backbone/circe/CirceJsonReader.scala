package backbone.circe

import backbone.circe.CirceJsonReader._
import backbone.json.{JsonReader, SnsEnvelope}
import cats.implicits._
import io.circe.Decoder
import io.circe.parser._
import org.slf4j.LoggerFactory

object CirceJsonReader {
  implicit val decodeSnsEnvelope: Decoder[SnsEnvelope] = Decoder.forProduct1("Message")(SnsEnvelope)
}

class CirceJsonReader extends JsonReader {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Option[SnsEnvelope] = {
    for {
      json <- parse(s)
        .map(_.some)
        .valueOr(f => {
          logger.error(s"Unable to parse json. reason=${f.message}")
          none
        })
      envelope <-
        json
          .as[SnsEnvelope]
          .map(Some(_))
          .valueOr(f => {
            logger.error(s"Unable to decode to SnsEnvelope. message=$s, reason=${f.message}")
            none
          })
    } yield envelope
  }
}
