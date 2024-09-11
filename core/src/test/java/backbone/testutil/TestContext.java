/*
 * Copyright (c) 2024 Backbone contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import software.amazon.awssdk.regions.Region;
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
            .region(Region.EU_CENTRAL_1)
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
