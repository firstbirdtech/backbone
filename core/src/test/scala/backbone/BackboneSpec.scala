package backbone

import backbone.format.Format
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar

import scala.util.{Success, Try}

class BackboneSpec extends WordSpec with DefaultTestContext with MockitoSugar {

  private implicit val snsClient = mock[AmazonSNSAsyncClient]

  "Backbone.consume" should {

    "doe something" in {

      val settings = ConsumerSettings(List.empty, List.empty, "Queue-name")
      val backbone = Backbone()

      implicit val format = new Format[String] {
        override def read(s: String): Try[String] = Success(s)
      }

      backbone.consume[String](settings) { s =>
        Consumed
      }

    }

  }

}
