package backbone.format

import backbone.MessageReader
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.TryValues._

import scala.util.{Success, Try}

class DefaultMessageReadsSpec extends FlatSpec with Matchers with DefaultMessageReads {

  "DefaultMessageReads" should "read a String to a Short value" in {
    testMessageReader[Short]("1").success.value shouldBe Some(1)
  }

  it should "read a String to a Int value" in {
    testMessageReader[Int]("1") shouldBe Success(Some(1))
  }

  it should "read a String to a Long value" in {
    testMessageReader[Long]("1") shouldBe Success(Some(1L))
  }

  it should "read a String to a Float value" in {
    testMessageReader[Float]("1.12") shouldBe Success(Some(1.12F))
  }

  it should "read a String to a Double value" in {
    testMessageReader[Double]("1.12") shouldBe Success(Some(1.12D))
  }

  it should "read a String to a Boolean value" in {
    testMessageReader[Boolean]("true") shouldBe Success(Some(true))
  }

  it should "read a String to a String value" in {
    testMessageReader[String]("message").success.value shouldBe Some("message")
  }

  it should "read a String to an array of Bytes" in {
    val actual = testMessageReader[Array[Byte]]("message").success.value
    actual should contain("message".getBytes)
  }

  it should "read a String to an array of Chars" in {
    testMessageReader[Array[Char]]("message").success.value should contain("message".toCharArray)
  }

  private[this] def testMessageReader[T](message: String)(implicit fo: MessageReader[T]): Try[Option[T]] =
    fo.read(message)

}
