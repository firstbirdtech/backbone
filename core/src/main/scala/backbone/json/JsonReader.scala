package backbone.json

import backbone.consumer.Consumer.MessageAction

/**
 * Trait which defines the JSON read methods which are used by Backbone. As Backbone tries to be independent of
 * any concrete JSON library implementations need to be in a different module.
 */
trait JsonReader {

  /**
   * Reads
   * @param s JSON String representation of an SnsEnvelope
   * @return Right(SnsEnvelope) if the String s could be parsed successfully
    *        Left(KeepMessage) if the String s could not be parsed into a SnsEnvelope
   */
  def readSnsEnvelope(s: String): Either[MessageAction, SnsEnvelope]
}
