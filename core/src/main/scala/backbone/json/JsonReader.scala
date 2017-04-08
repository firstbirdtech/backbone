package backbone.json

import backbone.consumer.Consumer.MessageAction

trait JsonReader {
  def read(s: String) : Either[MessageAction, SnsEnvelope]
}
