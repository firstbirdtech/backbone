package backbone.consumer
import akka.NotUsed
import akka.stream.scaladsl.Flow

object CountLimitation{
  def apply(n: Int): CountLimitation = new CountLimitation(n)
}
class CountLimitation(n: Int ) extends Limitation {
  override def limit[T]: Flow[T, T, NotUsed] = Flow[T].take(n)
}
