package backbone.consumer

import akka.NotUsed
import akka.stream.scaladsl.Flow

trait Limitation {

  def limit[T]: Flow[T, T, NotUsed]

}
