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

package backbone.consumer

import akka.NotUsed
import akka.stream.scaladsl.Flow

object CountLimitation {

  /**
   * Create a new CountLimitation
   *
   * @param n
   *   number of elements to consume before finishing
   * @return
   *   a CountLimitation
   */
  def apply(n: Int): CountLimitation = new CountLimitation(n)
}

/**
 * Limitation which finishes the consumption of events after n elements.
 *
 * @param n
 *   number of elements to consume before finishing
 */
class CountLimitation(n: Int) extends Limitation {
  override def limit[T]: Flow[T, T, NotUsed] = Flow[T].take(n)
}
