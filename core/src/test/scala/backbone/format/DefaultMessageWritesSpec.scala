package backbone.format

import backbone.MessageWriter
import backbone.testutil.BaseTest
import org.scalatest.flatspec.AnyFlatSpec

class DefaultMessageWritesSpec extends AnyFlatSpec with BaseTest with DefaultMessageWrites {

  "DefaultMessageWrites" should "read a String to a Short value" in {
    testMessageWriter[Short](1) mustBe "1"
  }

  it should "write a Int to a String value" in {
    testMessageWriter[Int](1) mustBe "1"
  }

  it should "write a Long to a String value" in {
    testMessageWriter[Long](1L) mustBe "1"
  }

  it should "write a Float to a String value" in {
    testMessageWriter[Float](1.12f) mustBe "1.12"
  }

  it should "write a Double to a String value" in {
    testMessageWriter[Double](1.12d) mustBe "1.12"
  }

  it should "write a Boolean to a String value" in {
    testMessageWriter[Boolean](true) mustBe "true"
  }

  it should "write a String to a String value" in {
    testMessageWriter[String]("message") mustBe "message"
  }

  it should "write an array of bytes to String value" in {
    testMessageWriter[Array[Byte]]("message".getBytes) mustBe "message"
  }

  it should "write an array of chars to String value" in {
    testMessageWriter[Array[Char]]("message".toCharArray) mustBe "message"
  }

  private[this] def testMessageWriter[T](message: T)(implicit fo: MessageWriter[T]): String = fo.write(message)

}
