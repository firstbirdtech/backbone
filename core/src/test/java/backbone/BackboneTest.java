package backbone;

import akka.Done;
import akka.actor.ActorRef;
import akka.stream.OverflowStrategy;
import akka.testkit.JavaTestKit;
import backbone.consumer.ConsumerSettings;
import backbone.javadsl.Backbone;
import backbone.publisher.PublisherSettings;
import backbone.testutil.IntegrationTest;
import com.amazonaws.services.sns.model.PublishRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import scala.Int;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BackboneTest extends IntegrationTest {

    private final Backbone backbone = Backbone.create(sqs, sns, system);
    private final PublisherSettings publisherSettings = PublisherSettings.create("topic-arn");

    @Test
    public void assertThatConsumeWorks() throws Exception {
        // TODO: provide consuming tests

        final ConsumerSettings consumerSettings = ConsumerSettings.create(Arrays.asList(), Arrays.asList(), "", 1, Optional.empty());

        final MessageReader<String> f = s -> s;

        backbone.consume(consumerSettings, f, (String s) -> Consumed.instance());
        assertTrue(true);
    }

    @Test
    public void publishAsync_oneMessage_messageSuccessfullyPublished() throws Exception {
        final Done result = backbone.publishAsync("message", publisherSettings, msg -> msg).get();

        assertThat(result, is(Done.getInstance()));
        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message")), any());
    }

    @Test
    public void publishAsync_collectionOfMessages_messagesSuccessfullyPublished() throws Exception {
        final List<String> messages = Arrays.asList("message-1", "message-2");
        final Done result = backbone.<String>publishAsync(messages, publisherSettings, msg -> msg).get();

        assertThat(result, is(Done.getInstance()));
        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-1")), any());
        verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-2")), any());
    }

    @Test
    public void actorPublisher_sendMultipleMessages_messagesSuccessfullyPublished() throws Exception {
        final ActorRef actorRef = backbone.<String>actorPublisher(publisherSettings, Int.MaxValue(), OverflowStrategy.dropHead(), msg -> msg);

        new JavaTestKit(system) {{
            new Within(duration("1 seconds")) {
                @Override
                protected void run() {
                    actorRef.tell("message-1", getRef());
                    expectNoMsg();
                    actorRef.tell("message-2", getRef());
                    expectNoMsg();

                    verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-1")), any());
                    verify(sns).publishAsync(eq(new PublishRequest(publisherSettings.topicArn(), "message-2")), any());
                }
            };
        }};
    }

}
