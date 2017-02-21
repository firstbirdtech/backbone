package backbone.testutil

import backbone.format.Format

import scala.util.{Success, Try}

object Implicits {
  implicit val stringFormat = new Format[String] {
    override def read(s: String): Try[String] = Success(s)
  }
}
