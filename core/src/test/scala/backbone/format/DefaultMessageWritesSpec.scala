package backbone.format

import backbone.MessageWriter
import org.scalatest.{FlatSpec, Matchers}

class DefaultMessageWritesSpec extends FlatSpec with Matchers with DefaultMessageWrites {

  "DefaultMessageWrites" should "read a String to a Short value" in {
    testMessageWriter[Short](1) shouldBe "1"
  }

  it should "write a Int to a String value" in {
    testMessageWriter[Int](1) shouldBe "1"
  }

  it should "write a Long to a String value" in {
    testMessageWriter[Long](1L) shouldBe "1"
  }

  it should "write a Float to a String value" in {
    testMessageWriter[Float](1.12F) shouldBe "1.12"
  }

  it should "write a Double to a String value" in {
    testMessageWriter[Double](1.12D) shouldBe "1.12"
  }

  it should "write a Boolean to a String value" in {
    testMessageWriter[Boolean](true) shouldBe "true"
  }

  it should "write a String to a String value" in {
    testMessageWriter[String]("message") shouldBe "message"
  }

  it should "write an array of bytes to String value" in {
    testMessageWriter[Array[Byte]]("message".getBytes) shouldBe "message"
  }

  it should "write an array of chars to String value" in {
    testMessageWriter[Array[Char]]("message".toCharArray) shouldBe "message"
  }

  private[this] def testMessageWriter[T](message: T)(implicit fo: MessageWriter[T]): String = fo.write(message)

}
