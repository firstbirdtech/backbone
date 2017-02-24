package backbone.consumer

import akka.NotUsed
import akka.stream.scaladsl.Flow

/** Defines an interface for limiting the consumption of elements of type T
 *
 */
trait Limitation {

  def limit[T]: Flow[T, T, NotUsed]

}
