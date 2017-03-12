package backbone.format

import org.scalatest.{Matchers, WordSpec}

class DefaultFormatsSpec extends WordSpec with Matchers with DefaultFormats {

  "DefaultFormats" should {

    "format a String to a Short value" in {
      testFormatter[Short]("1") shouldBe 1
    }

    "format a String to a Int value" in {
      testFormatter[Int]("1") shouldBe 1
    }

    "format a String to a Long value" in {
      testFormatter[Long]("1") shouldBe 1L
    }

    "format a String to a Float value" in {
      testFormatter[Float]("1.12") shouldBe 1.12F
    }

    "format a String to a Double value" in {
      testFormatter[Double]("1.12") shouldBe 1.12D
    }

    "format a String to a Boolean value" in {
      testFormatter[Boolean]("true") shouldBe true
    }

    "format a String to a String value" in {
      testFormatter[String]("message") shouldBe "message"
    }

    "format a String to aan array of Bytes" in {
      testFormatter[Array[Byte]]("message") shouldBe "message".getBytes
    }

    "format a String to an array of Chars" in {
      testFormatter[Array[Char]]("message") shouldBe "message".toCharArray
    }

  }

  private def testFormatter[T](message: String)(implicit fo: Format[T]): T = fo.read(message)

}
