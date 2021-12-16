package backbone.testutil;

import akka.actor.ActorSystem;
import backbone.javadsl.Backbone;
import com.github.matsluni.akkahttpspi.AkkaHttpClient;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class TestContext {

    protected static final SnsAsyncClient sns = mock(SnsAsyncClient.class);
    protected static ActorSystem system;
    protected static SQSRestServer server;
    protected static SqsAsyncClient sqs;
    protected static Backbone backbone;

    protected <T> T async(CompletableFuture<T> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Unable to await result of CompletableFuture.", e);
        }
    }

    @BeforeClass
    public static void beforeAll() {
        system = ActorSystem.create();
        server = SQSRestServerBuilder.withDynamicPort().start();
        final InetSocketAddress address = server.waitUntilStarted().localAddress();
        sqs = SqsAsyncClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
            .endpointOverride(URI.create("http://" + address.getHostName() + ":" + address.getPort()))
            .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
            .build();
        backbone = Backbone.create(sqs, sns, system);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        Await.ready(system.terminate(), Duration.create("5 seconds"));
        server.stopAndWait();
    }

    @Before
    public void beforeEach() {
        Mockito.reset(sns);

        when(sns.publish(any(PublishRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().build()));
    }

}
