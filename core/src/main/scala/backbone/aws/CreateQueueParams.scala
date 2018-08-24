package backbone.aws

case class CreateQueueParams(name: String, kmsKeyId: Option[String])
