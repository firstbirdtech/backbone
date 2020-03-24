package backbone

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import akka.{Done, NotUsed}
import akka.stream.{FlowShape, Graph}
import akka.stream.alpakka.sqs.SentTimestamp
import akka.stream.scaladsl.{Flow, Source}
import backbone.consumer._
import backbone.json.SnsEnvelope
import backbone.scaladsl.Backbone
import backbone.testutil.Implicits._
import backbone.testutil.{ElasticMQ, MockSNSAsyncClient, TestActorSystem}
import cats.implicits._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model._
import io.circe.syntax._
import org.mockito.Mockito.{spy, verify}
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class BackboneConsumeSpec
  extends WordSpec
    with ElasticMQ
    with MockSNSAsyncClient
    with MockitoSugar
    with ScalaFutures
    with MustMatchers
    with TestActorSystem {

  override implicit def patienceConfig: PatienceConfig = super.patienceConfig.copy(timeout = Span(3, Seconds))

  val backbone = Backbone()

  "Backbone.consume" should {

    "create a queue with the configured name" in {

      val settings = ConsumerSettings(Nil, "queue-name-1", None, 1, Some(CountLimitation(0)))

      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { res =>
        sqsClient.getQueueUrl("queue-name-1").getQueueUrl must be("http://localhost:9324/queue/queue-name-1")
      }

    }

    "create an encrypted queue with the configured name and kms key alias" in {

      val settings = ConsumerSettings(Nil, "queue-name-2", "arn:aws:kms:eu-central-1:123456789012:alias/TestAlias".some, 1, Some(CountLimitation(0)))

      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { res =>
        sqsClient.getQueueUrl("queue-name-2").getQueueUrl must be("http://localhost:9324/queue/queue-name-2")
      }

    }

    "fail parsing a wrongly formatted message and keep in on the queue" in {

      val message = new Message().withBody("blabla")
      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest("no-visibility").withAttributes(attributes.asJava)

      sqsClient.createQueue(createQueueRequest)
      sqsClient.sendMessage(new SendMessageRequest("http://localhost:9324/queue/no-visibility", message.getBody))

      val settings = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(1)))
      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }

    }

    "consume messages from the queue url" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))
      val f: Future[Done] = backbone.consume[String](settings)(s => Consumed)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }
    }

    "consume messages from the queue url if the MessageReader returns no event" in {
      sendMessage("message", "queue-name")

      val settings = ConsumerSettings(Nil, "queue-name", None, 1, Some(CountLimitation(1)))
      val reader = MessageReader(_ => Success(Option.empty[String]))

      val f: Future[Done] = backbone.consume[String](settings)(s => Rejected)(reader)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/queue-name").getMessages must have size 0
      }
    }

    "reject messages from the queue" in {
      sendMessage("message", "no-visibility")

      val settings = ConsumerSettings(Nil, "no-visibility", None, 1, Some(CountLimitation(0)), ReceiveSettings(0, 100, 10))
      val f: Future[Done] = backbone.consume[String](settings)(s => Rejected)

      whenReady(f) { _ =>
        sqsClient.receiveMessage("http://localhost:9324/queue/no-visibility").getMessages must have size 1
      }
    }

    "consume messages with a preprocessing handler" in {
      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      sendMessage("message1", queueName)
      sendMessage("message2", queueName)
      sendMessage("message3", queueName)

      val settings = ConsumerSettings(Nil, queueName, None, 3, Some(CountLimitation(3)), ReceiveSettings(5, 10, 10))

      val concurrentHashMap = new ConcurrentHashMap[String, String]()

      val f: Future[Done] = backbone.consume[String](settings)(
        s => {
          // store the values we receive in the map
          concurrentHashMap.put(s, s)
          Consumed
        },
        Flow[(MessageContext, String)].map { case (ctx, msg) => (ctx, Some(msg.toUpperCase())) }
      )

      whenReady(f) { _ =>
        // check if the values are there
        concurrentHashMap.get("MESSAGE1") mustBe "MESSAGE1"
        concurrentHashMap.get("MESSAGE2") mustBe "MESSAGE2"
        concurrentHashMap.get("MESSAGE3") mustBe "MESSAGE3"
      }
    }

  }
  "backbone.consumeAsync" should {

    "consume messages with a preprocessing handler" in {
      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      sendMessage("message1", queueName)
      sendMessage("message2", queueName)
      sendMessage("message3", queueName)

      val settings = ConsumerSettings(Nil, queueName, None, 3, Some(CountLimitation(3)), ReceiveSettings(5, 10, 10))

      val concurrentHashMap = new ConcurrentHashMap[String, String]()

      val f: Future[Done] = backbone.consumeAsync[String](settings)(
        s => {
          // store the values we receive in the map
          concurrentHashMap.put(s, s)
          Future.successful(Consumed)
        },
        Flow[(MessageContext, String)].map { case (ctx, msg) => (ctx, Some(msg.toUpperCase())) }
      )

      whenReady(f) { _ =>
        // check if the values are there
        concurrentHashMap.get("MESSAGE1") mustBe "MESSAGE1"
        concurrentHashMap.get("MESSAGE2") mustBe "MESSAGE2"
        concurrentHashMap.get("MESSAGE3") mustBe "MESSAGE3"
      }
    }

    "request additional message attributes and handle them" in {
      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      val id1 = sendMessage("message1", queueName)
      val id2 = sendMessage("message2", queueName)
      val id3 = sendMessage("message3", queueName)

      val concurrentHashMap = new ConcurrentHashMap[String, String]()

      val trackingLimitation = new Limitation {
        override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow[T]
          .collect{ case x: Message => x }
          .map(x => {
            concurrentHashMap.put(x.getMessageId, x.getAttributes.get("SentTimestamp"))
            x
          })
          .map(_.asInstanceOf[T])
          .take(3)
      }

      val settings = ConsumerSettings(
        Nil,
        queueName,
        None,
        3,
        Some(trackingLimitation),
        ReceiveSettings(5, 10, 10, Seq(SentTimestamp))
      )
      val f: Future[Done] = backbone.consumeAsync[String](settings)(s => Future.successful(Consumed))

      whenReady(f) { _ =>
        // check if the values are there
        Option(concurrentHashMap.get(id1)) mustBe defined
        Option(concurrentHashMap.get(id2)) mustBe defined
        Option(concurrentHashMap.get(id3)) mustBe defined
      }

    }

    "should delete a consumed message " in {

      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      val id1 = sendMessage("message1", queueName)
      val id2 = sendMessage("message2", queueName)
      val id3 = sendMessage("message3", queueName)

      val concurrentHashMap = new ConcurrentHashMap[String, String]()



      val trackingLimitation = new Limitation {
        override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow[T]
          .collect{ case x: Message => x }
          .map(x => {
            concurrentHashMap.put(x.getMessageId, x.getReceiptHandle)
            x
          })
          .take(3)
          .grouped(3)
          .map(grp => grp.sortBy(msg => msg.getAttributes.get("SentTimestamp").toLong))
          .map(grp => {
            grp
          })
          .flatMapConcat(grp => Source(grp))
          .map(_.asInstanceOf[T])
      }

      val settings = ConsumerSettings(
        Nil,
        queueName,
        None,
        1,
        Some(trackingLimitation),
        ReceiveSettings(5, 10, 10, Seq(SentTimestamp))
      )
      val sqsClientSpy = spy(sqsClient)
      val bb = Backbone.apply()(sqsClientSpy, snsClient, system)

      val f: Future[Done] = bb.consumeAsync[String](settings)(s => s match {
        case "message1" => Future.successful[ProcessingResult](Consumed)
        case "message2" => Future.successful[ProcessingResult](Rejected)
        case "message3" => Future.successful[ProcessingResult](Consumed)
        case _ => Future.successful[ProcessingResult](Consumed)
      })

      whenReady(f) { _ =>
        // check if the values are there
        Option(concurrentHashMap.get(id1)) mustBe defined
        Option(concurrentHashMap.get(id2)) mustBe defined
        Option(concurrentHashMap.get(id3)) mustBe defined
      }

      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMap.get(id1)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy, VerificationModeFactory.times(0)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMap.get(id2)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMap.get(id3)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
    }

    "should reject message with parse error " in {

      // this makes sure parsing fails on msg 2
      implicit val customStringFormat: MessageReader[String] = MandatoryMessageReader({
        case x@"message1" => Try(x)
        case x@"message2" => Failure(new IllegalArgumentException())
        case x@"message3" => Try(x)
      })

      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      val id1 = sendMessage("message1", queueName)
      val id2 = sendMessage("message2", queueName)
      val id3 = sendMessage("message3", queueName)

      val concurrentHashMapPassedTrough = new ConcurrentHashMap[String, String]()
      val concurrentHashMapEventHandler  = new ConcurrentHashMap[String, String]()

      val trackingLimitation = new Limitation {
        override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow[T]
          .collect{ case x: Message => x }
          .map(x => {
            concurrentHashMapPassedTrough.put(x.getMessageId, x.getReceiptHandle)
            x
          })
          .take(3)
          .grouped(3)
          .map(grp => grp.sortBy(msg => msg.getAttributes.get("SentTimestamp").toLong))
          .map(grp => {
            grp
          })
          .flatMapConcat(grp => Source(grp))
          .map(_.asInstanceOf[T])
      }

      val settings = ConsumerSettings(
        Nil,
        queueName,
        None,
        1,
        Some(trackingLimitation),
        ReceiveSettings(5, 10, 10, Seq(SentTimestamp))
      )
      val sqsClientSpy = spy(sqsClient)
      val bb = Backbone.apply()(sqsClientSpy, snsClient, system)

      val f: Future[Done] = bb.consumeAsync[String](settings) {
        case s: String => concurrentHashMapEventHandler.put(s, s); Future.successful(Consumed)
        case _ => Future.successful[ProcessingResult](Consumed)
      }(customStringFormat)


      whenReady(f) { _ =>
        // check if the values are there
        Option(concurrentHashMapPassedTrough.get(id1)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id2)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id3)) mustBe defined

        Option(concurrentHashMapEventHandler.get("message1")) mustBe defined
        Option(concurrentHashMapEventHandler.get("message2")) must not be defined
        Option(concurrentHashMapEventHandler.get("message3")) mustBe defined
      }

      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id1)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy, VerificationModeFactory.times(0)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id2)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id3)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
    }

    "should reject message that failed in consume " in {

      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      val id1 = sendMessage("message1", queueName)
      val id2 = sendMessage("message2", queueName)
      val id3 = sendMessage("message3", queueName)

      val concurrentHashMapPassedTrough = new ConcurrentHashMap[String, String]()
      val concurrentHashMapEventHandler  = new ConcurrentHashMap[String, String]()

      val trackingLimitation = new Limitation {
        override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow[T]
          .collect{ case x: Message => x }
          .map(x => {
            concurrentHashMapPassedTrough.put(x.getMessageId, x.getReceiptHandle)
            x
          })
          .take(3)
          .map(_.asInstanceOf[T])
      }

      val settings = ConsumerSettings(
        Nil,
        queueName,
        None,
        1,
        Some(trackingLimitation),
        ReceiveSettings(5, 10, 10, Seq(SentTimestamp))
      )
      val sqsClientSpy = spy(sqsClient)
      val bb = Backbone.apply()(sqsClientSpy, snsClient, system)

      val f: Future[Done] = bb.consumeAsync[String](settings) {
        case s@"message2" => concurrentHashMapEventHandler.put(s, s); Future.failed(new RuntimeException("something went wrong"))
        case s: String => concurrentHashMapEventHandler.put(s, s); Future.successful(Consumed)
        case _ => Future.successful[ProcessingResult](Consumed)
      }


      whenReady(f) { _ =>
        // check if the values are there
        Option(concurrentHashMapPassedTrough.get(id1)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id2)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id3)) mustBe defined

        Option(concurrentHashMapEventHandler.get("message1")) mustBe defined
        Option(concurrentHashMapEventHandler.get("message2")) mustBe defined
        Option(concurrentHashMapEventHandler.get("message3")) mustBe defined
      }

      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id1)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy, VerificationModeFactory.times(0)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id2)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id3)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
    }

    "should delete messages that got removed by the precondition filter " in {

      val queueName = "queue-name-" + UUID.randomUUID.toString

      val attributes = HashMap("VisibilityTimeout" -> "0")
      val createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes.asJava)
      sqsClient.createQueue(createQueueRequest)

      val id1 = sendMessage("message1", queueName)
      val id2 = sendMessage("message2", queueName)
      val id3 = sendMessage("message3", queueName)

      val concurrentHashMapPassedTrough = new ConcurrentHashMap[String, String]()
      val concurrentHashMapEventHandler  = new ConcurrentHashMap[String, String]()

      val trackingLimitation = new Limitation {
        override def limit[T]: Graph[FlowShape[T, T], NotUsed] = Flow[T]
          .collect{ case x: Message => x }
          .map(x => {
            concurrentHashMapPassedTrough.put(x.getMessageId, x.getReceiptHandle)
            x
          })
          .take(3)
          .map(_.asInstanceOf[T])
      }

      val settings = ConsumerSettings(
        Nil,
        queueName,
        None,
        1,
        Some(trackingLimitation),
        ReceiveSettings(5, 10, 10, Seq(SentTimestamp))
      )
      val sqsClientSpy = spy(sqsClient)
      val bb = Backbone.apply()(sqsClientSpy, snsClient, system)

      val f: Future[Done] = bb.consumeAsync[String](settings)( {
        case s: String => concurrentHashMapEventHandler.put(s, s); Future.successful(Rejected)
        case _ => Future.successful[ProcessingResult](Rejected)
      },
        Flow[(MessageContext, String)].map{
          case (ctx, s) if !"message2".equals(s) => (ctx, Some(s))
          case (ctx, s) => (ctx, None)
        }
      )


      whenReady(f) { _ =>
        // check if the values are there
        Option(concurrentHashMapPassedTrough.get(id1)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id2)) mustBe defined
        Option(concurrentHashMapPassedTrough.get(id3)) mustBe defined

        Option(concurrentHashMapEventHandler.get("message1")) mustBe defined
        Option(concurrentHashMapEventHandler.get("message2")) must not be defined
        Option(concurrentHashMapEventHandler.get("message3")) mustBe defined
      }

      verify(sqsClientSpy, VerificationModeFactory.times(0)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id1)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy, VerificationModeFactory.times(1)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id2)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
      verify(sqsClientSpy, VerificationModeFactory.times(0)).deleteMessageAsync(eqTo(s"http://localhost:9324/queue/$queueName"), eqTo(concurrentHashMapPassedTrough.get(id3)), any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]])
    }

  }

  private[this] def sendMessage(message: String, queue: String): String = {
    val envelope = SnsEnvelope(message)

    val sqsMessage = new Message().withBody(envelope.asJson.toString())
    val result = sqsClient.sendMessage(
      new SendMessageRequest(s"http://localhost:9324/queue/$queue", sqsMessage.getBody)
    )
    result.getMessageId
  }
}
