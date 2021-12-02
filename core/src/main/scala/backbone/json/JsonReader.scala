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

package backbone.json

/**
 * Trait which defines the JSON read methods which are used by Backbone. As Backbone tries to be independent of any
 * concrete JSON library implementations need to be in a different module.
 */
trait JsonReader {

  /**
   * Reads
   * @param s
   *   JSON String representation of an SnsEnvelope
   * @return
   *   Some(SnsEnvelope) if the String s could be parsed successfully None if the String s could not be parsed into a
   *   SnsEnvelope
   */
  def readSnsEnvelope(s: String): Option[SnsEnvelope]
}
