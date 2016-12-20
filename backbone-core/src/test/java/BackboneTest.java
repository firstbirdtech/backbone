import akka.actor.ActorSystem;
import backbone.format.Format;
import backbone.scaladsl.Backbone;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import org.junit.Test;
import scala.Function1;
import scala.util.Success;
import scala.util.Try;

public class BackboneTest {

    @Test
    public void name() throws Exception {
        final AmazonSQSAsyncClient sqs = new AmazonSQSAsyncClient();
        final AmazonSNSAsyncClient sns = new AmazonSNSAsyncClient();
        final ActorSystem actorSystem = ActorSystem.create();

        final Format format = (Format<String>) s -> new Success(s);

        final Backbone.ConsumerSettings settings = new Backbone.ConsumerSettings(null, null, "");
        final Function1<String, Backbone.ProcessingResult> f = new Function1<String, Backbone.ProcessingResult>() {
            @Override
            public Backbone.ProcessingResult apply(String v1) {
                return Backbone.Consumed$.MODULE$;
            }
        };
        Backbone.apply(sqs,sns).<String>consume(settings, f, actorSystem, format);


    }
}
