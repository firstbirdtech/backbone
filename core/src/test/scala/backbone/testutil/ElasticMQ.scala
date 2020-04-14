package backbone.testutil

import java.net.URI

import com.github.matsluni.akkahttpspi.AkkaHttpClient
import org.elasticmq.rest.sqs.{SQSRestServer, SQSRestServerBuilder}
import org.scalatest._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  CreateQueueRequest,
  ListQueuesRequest,
  PurgeQueueRequest,
  QueueAttributeName
}

import scala.jdk.CollectionConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.{Await, Future}

trait ElasticMQ extends TestSuiteMixin with BeforeAndAfterEach with BeforeAndAfterAll {
  this: TestSuite with BaseTest with TestActorSystem =>

  val elasticMqPort: Int    = 9324
  val server: SQSRestServer = SQSRestServerBuilder.withPort(elasticMqPort).start()

  implicit lazy val sqsClient: SqsAsyncClient = SqsAsyncClient
    .builder()
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
    .region(Region.EU_CENTRAL_1)
    .endpointOverride(URI.create(s"http://localhost:$elasticMqPort"))
    .httpClient(AkkaHttpClient.builder().withActorSystem(system).build())
    .build()

  abstract override protected def beforeAll(): Unit = {
    val request = CreateQueueRequest
      .builder()
      .queueName("queue-name")
      .attributes(Map(QueueAttributeName.VISIBILITY_TIMEOUT -> "1").asJava)
      .build()

    val result = sqsClient.createQueue(request).toScala
    Await.result(result, patienceConfig.timeout)
    super.beforeAll()
  }

  abstract override protected def afterAll(): Unit = {
    try super.afterAll()
    finally server.stopAndWait()
  }

  abstract override protected def beforeEach(): Unit = {
    val result = for {
      listResponse <- sqsClient.listQueues(ListQueuesRequest.builder().build()).toScala
      _ <- Future.sequence(
        listResponse
          .queueUrls()
          .asScala
          .map(url => sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).toScala)
      )
    } yield ()

    Await.result(result, patienceConfig.timeout)

    super.beforeEach()
  }

}
