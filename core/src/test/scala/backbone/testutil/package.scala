package backbone

import java.util.{Map => JMap}

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import com.amazonaws.services.sqs.model._

package object testutil {
  type SubscribeHandler          = AsyncHandler[SubscribeRequest, SubscribeResult]
  type CreateQueueHandler        = AsyncHandler[CreateQueueRequest, CreateQueueResult]
  type GetQueueAttributesHandler = AsyncHandler[GetQueueAttributesRequest, GetQueueAttributesResult]
  type SetQueueAttributesHandler = AsyncHandler[SetQueueAttributesRequest, SetQueueAttributesResult]
  type ReceiveMessagesHandler    = AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]
  type DeleteMessageHandler      = AsyncHandler[DeleteMessageRequest, DeleteMessageResult]
  type QueueAttributes           = JMap[String, String]
}
