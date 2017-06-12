package backbone.format

import backbone.{MandatoryMessageReader, MessageReader}

import scala.util.{Success, Try}

/**
 * Default Message reads for primitive data types.
 */
trait DefaultMessageReads {

  /**
   * Format to read a String value as a Short value.
   */
  implicit val shortFormat: MessageReader[Short] = MandatoryMessageReader(s => Try(s.toShort))

  /**
   * Format to read a String value as a Int value.
   */
  implicit val intFormat: MessageReader[Int] = MandatoryMessageReader(s => Try(s.toInt))

  /**
   * Format to read a String value as a Long value.
   */
  implicit val longFormat: MessageReader[Long] = MandatoryMessageReader(s => Try(s.toLong))

  /**
   * Format to read a String value as a Float value.
   */
  implicit val floatFormat: MessageReader[Float] = MandatoryMessageReader(s => Try(s.toFloat))

  /**
   * Format to read a String value as a Double value.
   */
  implicit val doubleFormat: MessageReader[Double] = MandatoryMessageReader(s => Try(s.toDouble))

  /**
   * Format to read a String value as a Boolean value.
   */
  implicit val booleanFormat: MessageReader[Boolean] = MandatoryMessageReader(s => Try(s.toBoolean))

  /**
   * Format to read a String value as a String value.
   */
  implicit val stringFormat: MessageReader[String] = MandatoryMessageReader(s => Success(s))

  /**
   * Format to read a String value as an array of Bytes.
   */
  implicit val byteFormat: MessageReader[Array[Byte]] = MandatoryMessageReader(s => Success(s.getBytes))

  /**
   * Format to read a String value as an array of Chars.
   */
  implicit val charFormat: MessageReader[Array[Char]] = MandatoryMessageReader(s => Success(s.toCharArray))

}
