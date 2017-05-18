package backbone.testutil

import java.util.concurrent.CompletableFuture

import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{Outcome, TestSuite, TestSuiteMixin}

trait MockSNSAsyncClient extends TestSuiteMixin with MockitoUtils { this: TestSuite =>

  implicit val snsClient = mock[AmazonSNSAsync]

  abstract override protected def withFixture(test: NoArgTest): Outcome = {

    when(snsClient.subscribeAsync(any[String], any[String], any[String], any[SubscribeHandler]))
      .thenAnswer(answer { invocation =>
        val result = new SubscribeResult()

        val handler = invocation.getArgument[SubscribeHandler](3)
        handler.onSuccess(new SubscribeRequest(), result)
        CompletableFuture.completedFuture(result)
      })

    try super.withFixture(test)
    finally Mockito.reset(snsClient)
  }
}
