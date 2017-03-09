package backbone.javadsl.consumer

import akka.NotUsed
import akka.stream.javadsl.Flow
import akka.stream.{FlowShape, Graph}
import backbone.consumer.Limitation

class CountLimitation(n:Long) extends Limitation {
  override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow.create[T]().take(n)
}

object CountLimitation {
  def create(n:Long):CountLimitation = new CountLimitation(n)
}

