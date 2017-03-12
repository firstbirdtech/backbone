package backbone.format

/**
 * Default Formats for primitive data types.
 */
trait DefaultFormats {

  /**
   * Format to read a String value as a Short value.
   */
  implicit val shortFormat: Format[Short] = new Format[Short] {
    override def read(s: String): Short = s.toShort
  }

  /**
   * Format to read a String value as a Int value.
   */
  implicit val intFormat: Format[Int] = new Format[Int] {
    override def read(s: String): Int = s.toInt
  }

  /**
   * Format to read a String value as a Long value.
   */
  implicit val longFormat: Format[Long] = new Format[Long] {
    override def read(s: String): Long = s.toLong
  }

  /**
   * Format to read a String value as a Float value.
   */
  implicit val floatFormat: Format[Float] = new Format[Float] {
    override def read(s: String): Float = s.toFloat
  }

  /**
   * Format to read a String value as a Double value.
   */
  implicit val doubleFormat: Format[Double] = new Format[Double] {
    override def read(s: String): Double = s.toDouble
  }

  /**
   * Format to read a String value as a Boolean value.
   */
  implicit val booleanFormat: Format[Boolean] = new Format[Boolean] {
    override def read(s: String): Boolean = s.toBoolean
  }

  /**
   * Format to read a String value as a String value.
   */
  implicit val stringFormat: Format[String] = new Format[String] {
    override def read(s: String): String = s
  }

  /**
   * Format to read a String value as an array of Bytes.
   */
  implicit val byteFormat: Format[Array[Byte]] = new Format[Array[Byte]] {
    override def read(s: String): Array[Byte]= s.getBytes
  }

  /**
   * Format to read a String value as an array of Chars.
   */
  implicit val charFormat: Format[Array[Char]] = new Format[Array[Char]] {
    override def read(s: String): Array[Char] = s.toCharArray
  }

}
