package backbone

import backbone.Backbone.Consumed
import backbone.format.Format
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
class BackboneSpec extends WordSpec with DefaultTestContext with MockitoSugar with ScalaFutures {

  private[this] implicit val snsClient = mock[AmazonSNSAsyncClient]

  implicit val format = new Format[String] {
    override def read(s: String): String = s
  }

  "Backbone.consume" should {

    "doe something" in {

      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")
      val backbone = Backbone()

      backbone.consume[String](settings) { s =>
        Consumed
      }

    }

  }

}
