package backbone.format

import backbone.MessageReader
import org.scalatest.{FlatSpec, Matchers}

class DefaultMessageReadsSpec extends FlatSpec with Matchers with DefaultMessageReads {

  it should "read a String to a Short value" in {
    testMessageReader[Short]("1") shouldBe 1
  }

  it should "read a String to a Int value" in {
    testMessageReader[Int]("1") shouldBe 1
  }

  it should "read a String to a Long value" in {
    testMessageReader[Long]("1") shouldBe 1L
  }

  it should "read a String to a Float value" in {
    testMessageReader[Float]("1.12") shouldBe 1.12F
  }

  it should "read a String to a Double value" in {
    testMessageReader[Double]("1.12") shouldBe 1.12D
  }

  it should "read a String to a Boolean value" in {
    testMessageReader[Boolean]("true") shouldBe true
  }

  it should "read a String to a String value" in {
    testMessageReader[String]("message") shouldBe "message"
  }

  it should "read a String to an array of Bytes" in {
    testMessageReader[Array[Byte]]("message") shouldBe "message".getBytes
  }

  it should "read a String to an array of Chars" in {
    testMessageReader[Array[Char]]("message") shouldBe "message".toCharArray
  }

  private[this] def testMessageReader[T](message: String)(implicit fo: MessageReader[T]): T = fo.read(message)

}
