package backbone.testutil

import java.util.concurrent.CompletableFuture

import org.mockito.Mockito
import org.scalatest.{Outcome, TestSuite, TestSuiteMixin}
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse, SubscribeRequest, SubscribeResponse}

trait MockSNSAsyncClient extends TestSuiteMixin { this: TestSuite with BaseTest =>

  implicit val snsClient: SnsAsyncClient = mock[SnsAsyncClient]

  abstract override protected def withFixture(test: NoArgTest): Outcome = {

    when(snsClient.subscribe(*[SubscribeRequest])).thenReturn {
      val response = SubscribeResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    when(snsClient.publish(*[PublishRequest])).thenReturn {
      val response = PublishResponse.builder().build()
      CompletableFuture.completedFuture(response)
    }

    try super.withFixture(test)
    finally Mockito.reset(snsClient)
  }
}
