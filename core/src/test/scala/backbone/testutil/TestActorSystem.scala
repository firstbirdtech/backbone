package backbone.testutil

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, TestSuite}

import scala.concurrent.ExecutionContext

trait TestActorSystem extends TestKitBase with BeforeAndAfterAll { this: TestSuite =>

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = system.dispatcher

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
