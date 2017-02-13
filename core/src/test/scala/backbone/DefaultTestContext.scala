package backbone

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DefaultTestContext extends BeforeAndAfterAll {
  this: Suite =>

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  implicit val sqsClient: AmazonSQSAsyncClient =
    new AmazonSQSAsyncClient(new BasicAWSCredentials("x", "x"))
      .withEndpoint[AmazonSQSAsyncClient]("http://localhost:9324")

  override protected def afterAll(): Unit = Await.ready(system.terminate(), 5.seconds)

}
