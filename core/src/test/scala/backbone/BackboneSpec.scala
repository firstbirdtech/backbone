package backbone

import backbone.format.Format
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

import scala.util.{Success, Try}
class BackboneSpec extends WordSpec with DefaultTestContext with MockitoSugar with ScalaFutures {

  private[this] implicit val snsClient = mock[AmazonSNSAsyncClient]

  implicit val format = new Format[String] {
    override def read(s: String): Try[String] = Success(s)
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
