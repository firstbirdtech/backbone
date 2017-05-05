package backbone.publisher

object PublisherSettings {

  def create(topicArn: String): PublisherSettings = PublisherSettings(topicArn)

}

/**
 *
 * @param topicArn the AWS topic ARN to publish to
 */
case class PublisherSettings(topicArn: String)
