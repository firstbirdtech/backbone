package backbone;

import akka.Done;
import akka.actor.ActorRef;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import backbone.consumer.ConsumerSettings;
import backbone.consumer.CountLimitation;
import backbone.publisher.PublisherSettings;
import backbone.testutil.TestContext;
import org.junit.Test;
import scala.Int;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

public class BackboneTest extends TestContext {

    private final PublisherSettings publisherSettings = PublisherSettings.create("topic-arn");

    @Test
    public void consume_oneMessage_messageConsumed() throws ExecutionException, InterruptedException {
        final ConsumerSettings consumerSettings = ConsumerSettings.create(new ArrayList<>(), "queue-name", Optional.empty(), 1, Optional.of(new CountLimitation(1)));
        async(sqs.createQueue(CreateQueueRequest.builder().queueName("queue-name").build()));
        async(sqs.sendMessage(SendMessageRequest.builder().queueUrl("http://localhost:9324/queue/queue-name").messageBody("\"message\":\"body\"").build()));
        final MessageReader<String> messageReader = MandatoryMessageReader.create(s -> s);
        backbone.consume(consumerSettings, messageReader, f -> Consumed.instance()).get();

        final ReceiveMessageResponse result = async(sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl("http://localhost:9324/queue/queue-name").build()));
        assertThat(result.messages().size(), is(0));
    }

    @Test
    public void publishAsync_oneMessage_messageSuccessfullyPublished() throws Exception {
        backbone.publishAsync("message", publisherSettings, msg -> msg).get();

        verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message").build());
    }

    @Test
    public void publishAsync_collectionOfMessages_messagesSuccessfullyPublished() throws Exception {
        final List<String> messages = Arrays.asList("message-1", "message-2");
        backbone.<String>publishAsync(messages, publisherSettings, msg -> msg).get();

        verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-1").build());
        verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-2").build());
    }

    @Test
    public void actorPublisher_sendMultipleMessages_messagesSuccessfullyPublished() throws Exception {
        final ActorRef actorRef = backbone.<String>actorPublisher(publisherSettings, Int.MaxValue(), OverflowStrategy.dropHead(), msg -> msg);

        new TestKit(system) {{
            actorRef.tell("message-1", getRef());
            actorRef.tell("message-2", getRef());

            awaitAssert(Duration.ofMillis(500), () -> {
                verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-1").build());
                verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-2").build());
                return null;
            });
        }};
    }

    @Test
    public void publisherSink_collectionOfMessages_messagesSuccessfullyPublished() throws Exception {
        final Sink<String, CompletableFuture<Done>> sink = backbone.publisherSink(publisherSettings, msg -> msg);

        final List<String> messages = Arrays.asList("message-1", "message-2");

        Source.from(messages)
            .runWith(sink, system)
            .get();

        verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-1").build());
        verify(sns).publish(PublishRequest.builder().topicArn(publisherSettings.topicArn()).message("message-2").build());
    }

}
