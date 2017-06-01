package backbone.testutil

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, TestSuite}

trait TestActorSystem extends TestKitBase with BeforeAndAfterAll { this: TestSuite =>

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer   = ActorMaterializer()

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
