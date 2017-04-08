package backbone;

import akka.actor.ActorSystem;
import backbone.consumer.ConsumerSettings;
import backbone.MessageReader;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

public class BackboneTest {

    private final ActorSystem system = ActorSystem.create();
    private final AmazonSQSAsyncClient sqs = new AmazonSQSAsyncClient();
    private final AmazonSNSAsyncClient sns = new AmazonSNSAsyncClient();
    @Test
    public void name() throws Exception {

        final backbone.javadsl.Backbone bb = backbone.javadsl.Backbone.create(sqs, sns, system);

        final ConsumerSettings consumerSettings = ConsumerSettings.create(Arrays.asList(), Arrays.asList(), "", 1, Optional.empty());

        final MessageReader<String> f= s -> s;

        bb.consume(consumerSettings, f, (String s) -> Consumed.instance());

    }
}
