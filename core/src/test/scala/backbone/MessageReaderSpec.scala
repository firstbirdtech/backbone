package backbone

import org.scalatest.TryValues._
import org.scalatest.{FlatSpec, MustMatchers}

import scala.compat.java8.FunctionConverters._
import scala.compat.java8.OptionConverters._
import scala.util.{Failure, Try}

class MessageReaderSpec extends FlatSpec with MustMatchers {

  "MessageReader" should "return the computed value" in {
    val reader                         = MessageReader(s => Try(Option(s)))
    val readValue: Try[Option[String]] = reader.read("message")
    readValue.success.value must contain("message")
  }

  "OptionalMessageReader" should "behave like the normalMessageReader" in {
    val reader                         = OptionalMessageReader(s => Try(Option(s)))
    val readValue: Try[Option[String]] = reader.read("message")
    readValue.success.value must contain("message")
  }

  it should "return a Failure if Java code throws an exception" in {
    val f : (String => Option[String]) = _ => throw new Exception
    val reader = OptionalMessageReader.create(f.andThen(_.asJava).asJava)
    reader.read("message") mustBe a[Failure[_]]
  }

  "MandatoryMessageReader" should "return Success(None) if the function returned null" in {
    val reader = MandatoryMessageReader(s => Try(null))
    val result = reader.read("message")
    result.success.value must not be null
    result.success.value mustBe empty
  }

  it should "return Success(Some()) if the function retured a value" in {
    val reader = MandatoryMessageReader(s => Try(s))
    val result = reader.read("message")
    result.success.value must contain("message")
  }

  it should "return a Failure if Java code throws an exception" in {
    val f : (String => String) = _ => throw new Exception
    val reader = MandatoryMessageReader.create(f.asJava)
    reader.read("message") mustBe a[Failure[_]]
  }

}
