package backbone.aws

case class CreateQueueParams(name: String, kmsKeyAlias: Option[String])
