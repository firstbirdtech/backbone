package backbone.circe

import backbone.circe.CirceJsonReader._
import backbone.consumer.Consumer
import backbone.json.{JsonReader, SnsEnvelope}
import cats.syntax.either._
import io.circe.Decoder
import io.circe.parser._
import org.slf4j.LoggerFactory

object CirceJsonReader {
  implicit val decodeSnsEnvelope: Decoder[SnsEnvelope] = Decoder.forProduct1("Message")(SnsEnvelope)
}

class CirceJsonReader extends JsonReader {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {
    for {
      json <- parse(s).leftMap(f => {
        logger.error(s"Unable to parse json. reason=${f.message}")
        Consumer.KeepMessage
      })
      envelope <- json
        .as[SnsEnvelope]
        .leftMap(f => {
          logger.error(s"Unable to decode to SnsEnvelope. message=$s, reason=${f.message}")
          Consumer.KeepMessage
        })
    } yield envelope
  }
}
