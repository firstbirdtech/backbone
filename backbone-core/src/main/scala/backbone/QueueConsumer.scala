package backbone

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.{Flow, Sink}
import backbone.aws.{AmazonSnsOps, AmazonSqsOps}
import backbone.format.Format
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

import scala.concurrent.Future
import scala.util.{Failure, Left, Right, Success}
import backbone.aws.Implicits._

class QueueConsumer(queueUrl: String, events: List[String])(implicit system: ActorSystem,
                                                            val sqs: AmazonSQSAsyncClient)
    extends AmazonSqsOps {

  import backbone.scaladsl.Backbone._
  import system._

  implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(_ => Supervision.resume)
  )

  def consumeAsync[T](parallelism: Int)(f: T => Future[ProcessingResult])(implicit fo: Format[T]): Future[Done] = {

    SqsSource(queueUrl)
      .mapAsync(parallelism) {implicit message =>
        parseMessage[T](message) match {
          case Left(a)  => Future.successful(a)
          case Right(t) => f(t).map(resultToAction)
        }
      }
      .runWith(ack)

  }

  def consume[T](f: T => ProcessingResult)(implicit fo: Format[T]): Future[Done] = {
    SqsSource(queueUrl).map {implicit message =>
      parseMessage[T](message) match {
        case Left(a)  => a
        case Right(t) => f.andThen(resultToAction)(t)
      }
    }.runWith(ack)
  }

  private def resultToAction(r: ProcessingResult)(implicit message: Message): MessageAction = r match {
    case Rejected     => KeepMessage
    case Consumed => RemoveMessage(message.getReceiptHandle)
  }

  private def parseMessage[T](message: Message)(implicit fo: Format[T]): Either[MessageAction, T] = {
    for {
      sns <- parse[SnsEnvelope](message.getBody).right
      _ <- {
        if (events.contains(sns.subject)) {
          Right(())
        } else {
          Left(RemoveMessage(message.getReceiptHandle))
        }
      }.right
      t <- (fo.read(sns.message) match {
        case Failure(exception) => Left[MessageAction,T](KeepMessage)
        case Success(value) => Right[MessageAction,T](value)
      }).right
    } yield t
  }

  private def ack: Sink[MessageAction, Future[Done]] = {
    Flow[MessageAction]
      .mapAsync(1) {
        case KeepMessage      => Future.successful(())
        case RemoveMessage(h) => deleteMessage(queueUrl, h)
      }
      .toMat(Sink.ignore)((_, f) => f)
  }

  private[this] def parse[A](s: String)(implicit r: Reads[A]): Either[MessageAction, A] = {
    Json.fromJson[A](Json.parse(s)) match {
      case JsSuccess(value, _) => Right(value)
      case JsError(_)          => Left(KeepMessage)
    }
  }

}
