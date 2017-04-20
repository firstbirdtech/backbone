package backbone;

import akka.actor.ActorSystem;
import backbone.consumer.ConsumerSettings;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

public class BackboneTest {

    private final ActorSystem system = ActorSystem.create();
    private final AmazonSQSAsync sqs = AmazonSQSAsyncClientBuilder.defaultClient();
    private final AmazonSNSAsync sns = AmazonSNSAsyncClientBuilder.defaultClient();
    @Test
    public void name() throws Exception {

        final backbone.javadsl.Backbone bb = backbone.javadsl.Backbone.create(sqs, sns, system);

        final ConsumerSettings consumerSettings = ConsumerSettings.create(Arrays.asList(), Arrays.asList(), "", 1, Optional.empty());

        final MessageReader<String> f= s -> s;

        bb.consume(consumerSettings, f, (String s) -> Consumed.instance());

    }
}
