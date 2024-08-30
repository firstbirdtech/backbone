/*
 * Copyright (c) 2021 Backbone contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package backbone.circe

import backbone.circe.CirceJsonReader._
import backbone.consumer.JsonReader
import cats.syntax.all._
import io.circe.Decoder
import io.circe.parser._
import org.slf4j.LoggerFactory

object CirceJsonReader {
  implicit val decodeSnsEnvelope: Decoder[JsonReader.SnsEnvelope] =
    Decoder.forProduct1("Message")(JsonReader.SnsEnvelope.apply)
}

class CirceJsonReader extends JsonReader {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Option[JsonReader.SnsEnvelope] = {
    for {
      json <- parse(s)
        .map(_.some)
        .valueOr(f => {
          logger.error(s"Unable to parse json. reason=${f.message}")
          none
        })
      envelope <-
        json
          .as[JsonReader.SnsEnvelope]
          .map(Some(_))
          .valueOr(f => {
            logger.error(s"Unable to decode to SnsEnvelope. message=$s, reason=${f.message}")
            none
          })
    } yield envelope
  }
}
