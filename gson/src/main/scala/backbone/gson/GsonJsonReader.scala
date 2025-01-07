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

package backbone.gson

import backbone.consumer.JsonReader
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory

import scala.util.Try

/**
 * JsonReader using gson
 */
class GsonJsonReader extends JsonReader {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  override def readSnsEnvelope(s: String): Option[JsonReader.SnsEnvelope] = {

    val maybeSnsEnvelope = for {
      json    <- Try(JsonParser.parseString(s)).map(_.getAsJsonObject).toOption
      message <- Option(json.get("Message")).map(_.getAsString)
    } yield JsonReader.SnsEnvelope(message)

    if (maybeSnsEnvelope.isEmpty) {
      logger.error(s"Unable to decode to SnsEnvelope. message=$s")
    }

    maybeSnsEnvelope
  }

}
