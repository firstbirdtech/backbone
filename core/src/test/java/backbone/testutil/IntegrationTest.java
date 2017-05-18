package backbone.testutil;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Supervision;
import akka.testkit.TestKit;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class IntegrationTest {

    protected static ActorSystem system;
    protected final ActorMaterializer mat = ActorMaterializer.create(ActorMaterializerSettings.create(system)
        .withSupervisionStrategy(Supervision.resumingDecider()), system);

    protected final AmazonSQSAsync sqs = AmazonSQSAsyncClientBuilder.defaultClient();
    protected final AmazonSNSAsync sns = mock(AmazonSNSAsyncClient.class);

    @Before
    public void beforeEach() {
        Mockito.reset(sns);

        when(sns.publishAsync(any(PublishRequest.class), any()))
            .thenAnswer(invocation -> {
                final PublishResult result = new PublishResult();

                final AsyncHandler<PublishRequest, PublishResult> handler = invocation.getArgument(1);
                handler.onSuccess(new PublishRequest(), result);
                return CompletableFuture.completedFuture(result);
            });
    }

    @BeforeClass
    public static void beforeAll() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void afterAll() {
        TestKit.shutdownActorSystem$default$2();
        system = null;
    }
}
