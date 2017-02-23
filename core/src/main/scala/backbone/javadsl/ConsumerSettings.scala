package backbone.javadsl

import java.util.{List => JList, Optional => JOption}

import backbone.consumer.Limitation

case class ConsumerSettings(events:JList[String],topics:JList[String],queue:String,parallelism:Integer,consumeWithin:JOption[Limitation]) {

}
