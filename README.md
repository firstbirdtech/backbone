# Backbone

![](https://github.com/firstbirdtech/backbone/workflows/CI/badge.svg)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/267982f6925248ef908962ddaf44632e)](https://www.codacy.com/gh/firstbirdtech/backbone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=firstbirdtech/backbone&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/267982f6925248ef908962ddaf44632e)](https://www.codacy.com/gh/firstbirdtech/backbone?utm_source=github.com&utm_medium=referral&utm_content=firstbirdtech/backbone&utm_campaign=Badge_Coverage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.firstbird/backbone-core_2.13/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Ccom.firstbird)  
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)


Backbone is a durable publish-subscribe platform built on top of Amazon AWS SQS and Amazon AWS SNS utilizing
Akka to stream events. This library currently is under active development, changes to the public API are still subject
to change.

Backbone provides nice high-level API for consuming events.

```scala
backbone.consume[String](settings){ str =>
    println(str)
    Consumed //or Rejected in case of an failure
}
```

## Quick Start

### Installation
To use backbone add the following dependency to your `build.sbt`.
```scala
libraryDependencies += "com.firstbird" %% "backbone-core"   % "X.Y.Z"
libraryDependencies += "com.firstbird" %% "backbone-circe"  % "X.Y.Z" //or any other JSON library
```

Or add the dependency to your `pom.xml`.
```xml
<dependency>
    <groupId>com.firstbird</groupId>
    <artifactId>backbone-core_2.13</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

### Consuming Events

To consume messages you have to specify the topics from which messages
should be consumed and a queue name that should be subscribed to the topics. The `parallelism` parameter
indicates how many messages are getting processed at a single time. To create an instance of `Backbone`
the only things you need are the according Amazon clients and a running `ActorSystem` which are
passed as implicit parameters.

When consuming of messages starts, Backbone subscribes the SQS queue automatically to the SNS topics
and sets the needed permissions on SQS side to give permissions to the SNS topics to send messages
to the queue.

```scala
import akka.actor._
import backbone._
import backbone.scaladsl._
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClient

implicit val system = ActorSystem()
val awsAkkaHttpClient = AkkaHttpClient.builder().withActorSystem(system).build()
implicit val sns = SnsAsyncClient.builder().httpClient(awsAkkaHttpClient).build()
implicit val sqs = SqsAsyncClient.builder().httpClient(awsAkkaHttpClient).build()

val backbone = Backbone()

val settings = ConsumerSettings(
    topics = "arn:aws:sns:eu-central-1:AWS_ACCOUNT_ID:topic-name" :: Nil,
    queue = "queue-name",
    parallelism = 5
)

//You need to define a MessageReader that tells Backbone how to decode the message body of the AWS SNS Message
implicit val messageReader = MessageReader(s => Success(Some(s)))

//Synchronous API
backbone.consume[String](settings){ str =>
    //process the event
    Consumed
}

//Asynchronous API
backbone.consumeAsync[String](settings){ _ =>
  Future.succesful(Consumed)
}
```

### Publishing Events

Backbone provides a couple of different methods which can be used to send messages to a Amazon AWS SNS Topic.
Basically they behave the same but provide an interface for various technologies as: `Future`s, Akka Streams,
Akka Actors.

```scala
import akka.actor._
import backbone._
import backbone.scaladsl._
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClient

implicit val system = ActorSystem()
val awsAkkaHttpClient = AkkaHttpClient.builder().withActorSystem(system).build()
implicit val sns = SnsAsyncClient.builder().httpClient(awsAkkaHttpClient).build()
implicit val sqs = SqsAsyncClient.builder().httpClient(awsAkkaHttpClient).build()

val backbone = Backbone()

val publishSettings = PublisherSettings("aws-sns-topic-arn")

//You need to define a MessageWriter that tells Backbone how to encode the message body of the AWS SNS Message
implicit val writer = MessageWriter(s => s)

//Actor Publisher
val actor: ActorRef = backbone.actorPublisher[String](publishSettings)
actor ! "send this to sns"

//Akka Streams Sink
implicit val mat = ActorMaterializer()

val sink = backbone.publisherSink[String](publishSettings)
Source.single("send this to sns").to(sink)

//Async Publish
val f: Future[PublishResult] = backbone.publishAsync[String]("send this to sns", publishSettings)
val f1: Future[PublishResult] = backbone.publishAsync[String]("send this to sns" :: "and this" :: Nil, publishSettings)
```

## AWS Policies

To work properly the AWS user used for running Backbone needs special permission which are being set
by following example policies.

To allow publishing of events via Backbone the AWS user needs the right to publish to the SNS topic.
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmt1474367067000",
            "Effect": "Allow",
            "Action": [
                "sns:*"
            ],
            "Resource": [
                "{topicArnToPublishTo}"
            ]
        }
    ]
}
```

To allow Backbone consuming events from the configured queue the AWS user needs full permissions for the
queue that should be created for consuming messages as well as permissions to subscribe and unsubscribe
the queue to the configured SNS topics.
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmt1474984885000",
            "Effect": "Allow",
            "Action": [
                "sqs:*"
            ],
            "Resource": [
                "arn:aws:sqs:eu-central-1:{awsAccountId}:{queueName}"
            ]
        },
        {
            "Sid": "Stmt1474985033000",
            "Effect": "Allow",
            "Action": [
                "sns:Subscribe",
                "sns:Unsubscribe"
            ],
            "Resource": [
                "{topicArn1}",
                "{topicArn2}"
            ]
        }
    ]
}
```

### JSON Modules

The core module of backbone is completely independent of a JSON library (because there are many around in the JVM ecosystem).
You can configure which library you want to use by adding one of the following to the classpath. 

```scala
libraryDependencies += "com.firstbird" %% "backbone-circe"      % "X.Y.Z"
libraryDependencies += "com.firstbird" %% "backbone-play-json"  % "X.Y.Z"
libraryDependencies += "com.firstbird" %% "backbone-gson"       % "X.Y.Z"
```

## Running the Tests

Run the tests from sbt with:
```sbt
test
```

## Feedback
We very much appreciate feedback, please open an issue and/or create a PR.

## Contributors

*   Pedro Dias
*   [Fabian Grutsch](https://github.com/fgrutsch)
*   Georgi Lichev
*   [Daniel Pfeiffer](https://github.com/dpfeiffer)
