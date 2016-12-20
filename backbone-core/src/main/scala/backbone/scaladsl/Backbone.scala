package backbone.scaladsl

import akka.Done
import akka.actor.ActorSystem
import backbone.QueueConsumer
import backbone.aws.{AmazonSnsOps, AmazonSqsOps}
import backbone.format.Format
import backbone.scaladsl.Backbone.{ConsumerSettings, ProcessingResult, QueueInformation}
import com.amazonaws.auth.policy.actions.SQSActions
import com.amazonaws.auth.policy.conditions.ConditionFactory
import com.amazonaws.auth.policy.{Policy, Principal, Resource, Statement}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClient

import scala.concurrent.{ExecutionContext, Future}

object Backbone {

  case class QueueInformation(url: String, arn: String)

  sealed trait ProcessingResult
  case object Rejected     extends ProcessingResult
  case object Consumed extends ProcessingResult

  sealed trait MessageAction
  case class RemoveMessage(receiptHandle: String) extends MessageAction
  case object KeepMessage                         extends MessageAction

  case class SnsEnvelope(subject: String, message: String)
  case class Envelope[P](deleteHandle: String, subject: String, payload: P)
  case class ConsumerSettings(events: List[String], topics: List[String], queue: String)

  def apply()(implicit sqs: AmazonSQSAsyncClient,sns: AmazonSNSAsyncClient): Backbone = new Backbone()

}

class Backbone(implicit val sqs: AmazonSQSAsyncClient, val sns: AmazonSNSAsyncClient)
    extends AmazonSqsOps
    with AmazonSnsOps {

  def consume[T](settings: ConsumerSettings)(f: T => ProcessingResult)(implicit system: ActorSystem,
      fo: Format[T]) : Future[Done] = {
    consume[T](1,settings)(f.andThen(Future.successful))
  }

  def consume[T](parallelism: Int, settings: ConsumerSettings)(f: T => Future[ProcessingResult])(implicit system: ActorSystem,
      fo: Format[T]): Future[Done] = {
    implicit val ec = system.dispatcher

    for {
      queue <- createQueue(settings.queue)
      _     <- subscribe(queue, settings.topics)
      r     <- new QueueConsumer(queue.url, settings.events).consumeAsync(parallelism)(f)
    } yield r

  }

  private[this] def subscribe(queue: QueueInformation, topics: List[String])(
      implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- updatePolicy(queue, topics)
      _ <- Future.sequence(topics.map(t => subscribe(queue, t)))
    } yield ()
  }

  private[this] def updatePolicy(queue: QueueInformation, topics: List[String])(
      implicit ec: ExecutionContext): Future[Unit] = {
    topics match {
      case Nil => Future.successful(())
      case ts  => savePolicy(queue.url, createPolicy(queue.arn, ts))
    }
  }

  private[this] def createPolicy(queueArn: String, topicsArns: Seq[String]): Policy = {
    val statements = topicsArns.map { arn =>
      new Statement(Statement.Effect.Allow)
        .withId("topic-subscription-" + arn)
        .withPrincipals(Principal.AllUsers)
        .withActions(SQSActions.SendMessage)
        .withResources(new Resource(queueArn))
        .withConditions(ConditionFactory.newSourceArnCondition(arn))
    }

    new Policy().withStatements(statements: _*)
  }

}
