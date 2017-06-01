package backbone.testutil

import java.util.concurrent.CompletableFuture
import java.util.{List => JList}

import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{Outcome, TestSuite, TestSuiteMixin}

import scala.collection.JavaConverters._

trait MockSQSAsyncClient extends TestSuiteMixin with MockitoUtils { this: TestSuite =>

  implicit val sqsClient: AmazonSQSAsyncClient = mock[AmazonSQSAsyncClient]

  abstract override protected def withFixture(test: NoArgTest): Outcome = {

    when(sqsClient.createQueueAsync(any[String], any[CreateQueueHandler])).thenAnswer(answer { i =>
      val result = new CreateQueueResult()
        .withQueueUrl("queue-url")

      val handler = i.getArgument[CreateQueueHandler](1)
      handler.onSuccess(new CreateQueueRequest(), result)
      CompletableFuture.completedFuture(result)
    })

    when(sqsClient.getQueueAttributesAsync(any[String], any[JList[String]], any[GetQueueAttributesHandler]))
      .thenAnswer(answer { i =>
        val result = new GetQueueAttributesResult()
          .withAttributes(Map(QueueAttributeName.QueueArn.toString -> "queue-arn").asJava)
        val handler = i.getArgument[GetQueueAttributesHandler](2)
        handler.onSuccess(new GetQueueAttributesRequest(), result)
        CompletableFuture.completedFuture(result)
      })

    when(sqsClient.setQueueAttributesAsync(any[String], any[QueueAttributes], any[SetQueueAttributesHandler]))
      .thenAnswer(answer { i =>
        val handler = i.getArgument[SetQueueAttributesHandler](2)
        val result  = new SetQueueAttributesResult
        handler.onSuccess(new SetQueueAttributesRequest, result)
        CompletableFuture.completedFuture(result)
      })

    when(sqsClient.deleteMessageAsync(any[String], any[String], any[DeleteMessageHandler])).thenAnswer(answer { i =>
      val handler = i.getArgument[DeleteMessageHandler](2)
      val result  = new DeleteMessageResult
      handler.onSuccess(new DeleteMessageRequest, result)
      CompletableFuture.completedFuture(result)
    })

    try super.withFixture(test)
    finally Mockito.reset(sqsClient)
  }

  def withNoMessages(testCode: => Any): Any = withMessages()(testCode)

  def withMessages(messages: List[Message] = Nil)(testCode: => Any): Any = {
    when(sqsClient.receiveMessageAsync(any[ReceiveMessageRequest], any[ReceiveMessagesHandler]))
      .thenAnswer(answer { i =>
        val handler = i.getArgument[ReceiveMessagesHandler](1)

        val result = new ReceiveMessageResult()
          .withMessages(messages.asJava)

        handler.onSuccess(new ReceiveMessageRequest, result)
        CompletableFuture.completedFuture(result)
      })

    testCode
  }
}
