import akka.NotUsed
import akka.stream.scaladsl.Flow
import backbone.consumer.MessageContext

package object backbone {
  type Preprocessor[T] = Flow[(MessageContext,T), (MessageContext, Option[T]), NotUsed]
}
