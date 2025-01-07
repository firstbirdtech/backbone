/*
 * Copyright (c) 2025 Backbone contributors
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

package backbone.playjson

import backbone.consumer.JsonReader
import org.slf4j.LoggerFactory
import play.api.libs.json
import play.api.libs.json._

class PlayJsonReader extends JsonReader {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] implicit val snsEnvelopeReads: json.Reads[JsonReader.SnsEnvelope] = (__ \ "Message")
    .read[String]
    .map(JsonReader.SnsEnvelope.apply)

  override def readSnsEnvelope(s: String): Option[JsonReader.SnsEnvelope] = {
    Json.fromJson[JsonReader.SnsEnvelope](Json.parse(s)) match {
      case JsSuccess(value, _) => Some(value)
      case JsError(errors) =>
        logger.error(s"Unable to decode to SnsEnvelope. message=$s, errors=$errors")
        None
    }

  }
}
