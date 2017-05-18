package backbone.testutil

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.{CreateQueueRequest, PurgeQueueRequest}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClient}
import org.elasticmq.rest.sqs.{SQSRestServer, SQSRestServerBuilder}
import org.scalatest._

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap

trait ElasticMQ extends TestSuiteMixin with BeforeAndAfterEach with BeforeAndAfterAll { this: TestSuite =>

  val elasticMqPort: Int            = 9324
  private var server: SQSRestServer = _

  implicit lazy val sqsClient: AmazonSQSAsync = AmazonSQSAsyncClient
    .asyncBuilder()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(new EndpointConfiguration(s"http://localhost:$elasticMqPort", "eu-central-1"))
    .build()
    .asInstanceOf[AmazonSQSAsyncClient]

  abstract override protected def beforeAll(): Unit = {
    server = SQSRestServerBuilder.withPort(elasticMqPort).start()
    sqsClient.createQueue(new CreateQueueRequest("queue-name").withAttributes(HashMap("VisibilityTimeout"->"1").asJava))
    super.beforeAll()
  }

  abstract override protected def afterAll(): Unit = {
    try super.afterAll()
    finally server.stopAndWait()
  }

  abstract override protected def beforeEach(): Unit = {
    sqsClient.listQueues().getQueueUrls.asScala.foreach { url =>
      sqsClient.purgeQueue(new PurgeQueueRequest(url))
    }
    super.beforeEach()
  }

}
