package backbone.testutil

import backbone.consumer.Consumer
import backbone.json.{JsonReader, SnsEnvelope}
import backbone.testutil.Implicits._
import cats.syntax.either._
import io.circe.parser.parse

class TestJsonReader extends JsonReader {

  override def readSnsEnvelope(s: String): Either[Consumer.MessageAction, SnsEnvelope] = {
    for {
      json     <- parse(s).leftMap(_ => Consumer.KeepMessage)
      envelope <- json.as[SnsEnvelope].leftMap(_ => Consumer.KeepMessage)
    } yield envelope
  }
}
