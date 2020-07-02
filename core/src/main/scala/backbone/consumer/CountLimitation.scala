package backbone.consumer
import akka.NotUsed
import akka.stream.scaladsl.Flow

object CountLimitation {

  /**
   * Create a new CountLimitation
   *
   * @param n number of elements to consume before finishing
   * @return a CountLimitation
   */
  def apply(n: Int): CountLimitation = new CountLimitation(n)
}

/**
 * Limitation which finishes the consumption of events after n elements.
 *
 * @param n number of elements to consume before finishing
 */
class CountLimitation(n: Int) extends Limitation {
  override def limit[T]: Flow[T, T, NotUsed] = Flow[T].take(n)
}
