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
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.Test;
import scala.Int;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class BackboneTest extends TestContext {

    private final PublisherSettings publisherSettings = PublisherSettings.create("topic-arn");

    @Test
    public void consume_oneMessage_messageConsumed() throws ExecutionException, InterruptedException {

        final ConsumerSettings consumerSettings = ConsumerSettings.create(new ArrayList<String>(), "queue-name", 1, Optional.of(new CountLimitation(1)));
        sqs.createQueue("queue-name");
        sqs.sendMessage(new SendMessageRequest("http://localhost:9324/queue/queue-name", "\"message\":\"body\""));
        final MessageReader<String> messageReader = MandatoryMessageReader.create(s -> s);
        backbone.consume(consumerSettings, messageReader, f -> Consumed.instance()).get();

        assertThat(sqs.receiveMessage("http://localhost:9324/queue/queue-name").getMessages().size(), is(0));
    }

    @Test
    public void publishAsync_oneMessage_messageSuccessfullyPublished() throws Exception {
        backbone.publishAsync("message", publisherSettings, msg -> msg).get();

        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message")), any());
    }

    @Test
    public void publishAsync_collectionOfMessages_messagesSuccessfullyPublished() throws Exception {
        final List<String> messages = Arrays.asList("message-1", "message-2");
        backbone.<String>publishAsync(messages, publisherSettings, msg -> msg).get();

        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-1")), any());
        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-2")), any());
    }

    @Test
    public void actorPublisher_sendMultipleMessages_messagesSuccessfullyPublished() throws Exception {
        final ActorRef actorRef = backbone.<String>actorPublisher(publisherSettings, Int.MaxValue(), OverflowStrategy.dropHead(), msg -> msg);

        new TestKit(system) {{
            actorRef.tell("message-1", getRef());
            actorRef.tell("message-2", getRef());

            awaitAssert(duration("500 millis"), () -> {
                verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-1")), any());
                return verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-2")), any());
            });
        }};
    }

    @Test
    public void publisherSink_collectionOfMessages_messagesSuccessfullyPublished() throws Exception {
        final Sink<String, CompletableFuture<Done>> sink = backbone.publisherSink(publisherSettings, msg -> msg);

        final List<String> messages = Arrays.asList("message-1", "message-2");

        Source.from(messages)
            .runWith(sink, mat)
            .get();

        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-1")), any());
        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-2")), any());
    }

}
