package backbone.generic.json

import backbone.format.Format
import io.circe.generic.decoding.DerivedDecoder
import io.circe.parser.parse
import shapeless.Lazy

import scala.util.Success

object Implicits {

  implicit def deriveFormat[A](implicit decode: Lazy[DerivedDecoder[A]]): Format[A] = new Format[A] {
    override def read(s: String): Success[A] = {

      val json = parse(s).right.get
      val r = decode.value.decodeJson(json).right.get
      Success(r)
    }
  }

}
