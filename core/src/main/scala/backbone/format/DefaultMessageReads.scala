package backbone.format

import backbone.MessageReader

/**
 * Default Message reads for primitive data types.
 */
trait DefaultMessageReads {

  /**
   * Format to read a String value as a Short value.
   */
  implicit val shortFormat: MessageReader[Short] = new MessageReader[Short] {
    override def read(s: String): Short = s.toShort
  }

  /**
   * Format to read a String value as a Int value.
   */
  implicit val intFormat: MessageReader[Int] = new MessageReader[Int] {
    override def read(s: String): Int = s.toInt
  }

  /**
   * Format to read a String value as a Long value.
   */
  implicit val longFormat: MessageReader[Long] = new MessageReader[Long] {
    override def read(s: String): Long = s.toLong
  }

  /**
   * Format to read a String value as a Float value.
   */
  implicit val floatFormat: MessageReader[Float] = new MessageReader[Float] {
    override def read(s: String): Float = s.toFloat
  }

  /**
   * Format to read a String value as a Double value.
   */
  implicit val doubleFormat: MessageReader[Double] = new MessageReader[Double] {
    override def read(s: String): Double = s.toDouble
  }

  /**
   * Format to read a String value as a Boolean value.
   */
  implicit val booleanFormat: MessageReader[Boolean] = new MessageReader[Boolean] {
    override def read(s: String): Boolean = s.toBoolean
  }

  /**
   * Format to read a String value as a String value.
   */
  implicit val stringFormat: MessageReader[String] = new MessageReader[String] {
    override def read(s: String): String = s
  }

  /**
   * Format to read a String value as an array of Bytes.
   */
  implicit val byteFormat: MessageReader[Array[Byte]] = new MessageReader[Array[Byte]] {
    override def read(s: String): Array[Byte] = s.getBytes
  }

  /**
   * Format to read a String value as an array of Chars.
   */
  implicit val charFormat: MessageReader[Array[Char]] = new MessageReader[Array[Char]] {
    override def read(s: String): Array[Char] = s.toCharArray
  }

}
