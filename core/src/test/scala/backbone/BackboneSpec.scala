package backbone

import backbone.format.Format
import backbone.scaladsl.Backbone
import backbone.scaladsl.Backbone.{Consumed, ConsumerSettings}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import org.scalatest.WordSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.stubbing.Answer
import scala.util.{Success, Try}
import java.util.concurrent.{CompletableFuture, Future => JFuture}

import backbone.consumer.CountLimitation
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.model.{SubscribeRequest, SubscribeResult}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import scala.concurrent.Await
class BackboneSpec extends WordSpec with DefaultTestContext with MockitoSugar with ScalaFutures {

  private implicit val snsClient = mock[AmazonSNSAsyncClient]

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
