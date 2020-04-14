package backbone.format

import backbone.MessageReader
import backbone.testutil.BaseTest
import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.{Success, Try}

class DefaultMessageReadsSpec extends AnyFlatSpec with BaseTest with DefaultMessageReads {

  "DefaultMessageReads" should "read a String to a Short value" in {
    testMessageReader[Short]("1").success.value mustBe Some(1)
  }

  it should "read a String to a Int value" in {
    testMessageReader[Int]("1") mustBe Success(Some(1))
  }

  it should "read a String to a Long value" in {
    testMessageReader[Long]("1") mustBe Success(Some(1L))
  }

  it should "read a String to a Float value" in {
    testMessageReader[Float]("1.12") mustBe Success(Some(1.12f))
  }

  it should "read a String to a Double value" in {
    testMessageReader[Double]("1.12") mustBe Success(Some(1.12d))
  }

  it should "read a String to a Boolean value" in {
    testMessageReader[Boolean]("true") mustBe Success(Some(true))
  }

  it should "read a String to a String value" in {
    testMessageReader[String]("message").success.value mustBe Some("message")
  }

  it should "read a String to an array of Bytes" in {
    val actual = testMessageReader[Array[Byte]]("message").success.value
    actual must contain("message".getBytes)
  }

  it must "read a String to an array of Chars" in {
    testMessageReader[Array[Char]]("message").success.value must contain("message".toCharArray)
  }

  private[this] def testMessageReader[T](message: String)(implicit fo: MessageReader[T]): Try[Option[T]] =
    fo.read(message)

}
