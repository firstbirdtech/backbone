package backbone

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DefaultTestContext extends BeforeAndAfterAll { this: Suite =>

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()

  implicit val sqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:9324", "eu-central-1"))
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .build()

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
    sqsClient.shutdown()
  }

}
