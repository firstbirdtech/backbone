package backbone.testutil

import java.util.concurrent.CompletableFuture

import org.mockito.Mockito
import org.scalatest.{Outcome, TestSuite, TestSuiteMixin}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import scala.jdk.CollectionConverters._

trait MockSQSAsyncClient extends TestSuiteMixin { this: TestSuite with BaseTest =>

  implicit val sqsClient: SqsAsyncClient = mock[SqsAsyncClient]

  abstract override protected def withFixture(test: NoArgTest): Outcome = {

    when(sqsClient.createQueue(*[CreateQueueRequest])).thenReturn {
      val result = CreateQueueResponse.builder().queueUrl("queue-url").build()
      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.getQueueAttributes(any[GetQueueAttributesRequest])).thenReturn {
      val result = GetQueueAttributesResponse
        .builder()
        .attributes(Map(QueueAttributeName.QUEUE_ARN -> "queue-arn").asJava)
        .build()

      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.setQueueAttributes(any[SetQueueAttributesRequest])).thenReturn {
      val result = SetQueueAttributesResponse.builder().build()
      CompletableFuture.completedFuture(result)
    }

    when(sqsClient.deleteMessage(any[DeleteMessageRequest])).thenReturn {
      val result = DeleteMessageResponse.builder().build()
      CompletableFuture.completedFuture(result)
    }

    try super.withFixture(test)
    finally Mockito.reset(sqsClient)
  }

  def withNoMessages(testCode: => Any): Any = withMessages()(testCode)

  def withMessages(messages: List[Message] = Nil)(testCode: => Any): Any = {
    when(sqsClient.receiveMessage(any[ReceiveMessageRequest])).thenReturn {
      val result = ReceiveMessageResponse.builder().messages(messages.asJava).build()
      CompletableFuture.completedFuture(result)
    }

    testCode
  }
}
