package backbone.consumer

import akka.NotUsed
import akka.stream.{FlowShape, Graph}

trait Limitation {

  def limit[T]: Graph[FlowShape[T, T], NotUsed]

}
