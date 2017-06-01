package backbone.testutil;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Supervision;
import backbone.javadsl.Backbone;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TestContext {

    protected final Integer elasticMqPort = 9324;
    protected final AmazonSQSAsync sqs = AmazonSQSAsyncClient.asyncBuilder()
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:" + elasticMqPort, "eu-central-1"))
        .build();
    protected final AmazonSNSAsync sns = mock(AmazonSNSAsyncClient.class);
    protected ActorSystem system;
    protected ActorMaterializer mat;
    protected SQSRestServer server = null;
    protected Backbone backbone;

    @Before
    public void beforeEach() {
        system = ActorSystem.create();
        mat = ActorMaterializer.create(ActorMaterializerSettings.create(system)
            .withSupervisionStrategy(Supervision.resumingDecider()), system);
        server = SQSRestServerBuilder.withPort(elasticMqPort).start();
        backbone = Backbone.create(sqs, sns, system);
        Mockito.reset(sns);

        when(sns.publishAsync(any(PublishRequest.class), any()))
            .thenAnswer(invocation -> {
                final PublishResult result = new PublishResult();

                final AsyncHandler<PublishRequest, PublishResult> handler = invocation.getArgument(1);
                handler.onSuccess(new PublishRequest(), result);
                return CompletableFuture.completedFuture(result);
            });
    }

    @After
    public void afterEach() throws Exception {
        Await.ready(system.terminate(), Duration.create("1 second"));
        server.stopAndWait();
    }

}
