package backbone.consumer

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{FlowShape, Graph}
import com.amazonaws.services.sqs.model.Message

/**
  * Allows to customize the flow before its applied to the consumer.
  *
  * Do NOT use filter in this flow. Use the Option instead, like this:
  *
  * Return (Message, None) if you want this Event to be rejected,
  * return (Message, Some(e)) if you want this event to be processed by your consumer.
  *
 */
trait CustomSuccessFlow[T] {

  def flow(): Graph[FlowShape[(MessageContext, T), (MessageContext, Option[T])], NotUsed]

  def default: Flow[(MessageContext, T), (MessageContext, Option[T]), NotUsed] = Flow[(MessageContext, T)].map(x => (x._1, Some(x._2)))

}
