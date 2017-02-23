package backbone.javadsl.consumer

import java.util.{List => JList, Optional => JOption}
import backbone.consumer.Limitation

case class Settings(queueUrl: String, event: JList[String], parallelism: Integer, limitation: JOption[Limitation]) {}
