package backbone.consumer

import akka.NotUsed
import akka.stream.{FlowShape, Graph}

/** Defines an interface for limiting the consumption of elements of type T
 *
 */
trait Limitation {

  def limit[T]: Graph[FlowShape[T, T], NotUsed]

}
