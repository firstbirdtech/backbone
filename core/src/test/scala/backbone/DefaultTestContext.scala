package backbone

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DefaultTestContext extends BeforeAndAfterAll { this: Suite =>

  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()


  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
    }

}
