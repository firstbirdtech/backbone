# Backbone

[![Build Status](https://travis-ci.org/firstbirdtech/backbone.svg?branch=master)](https://travis-ci.org/firstbirdtech/backbone)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/805331624b64414ab4bebae67557d5f7)](https://www.codacy.com/app/daniel-pfeiffer/backbone?utm_source=github.com&utm_medium=referral&utm_content=firstbirdtech/backbone&utm_campaign=Badge_Coverage)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/805331624b64414ab4bebae67557d5f7)](https://www.codacy.com/app/daniel-pfeiffer/backbone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=firstbirdtech/backbone&amp;utm_campaign=Badge_Grade)

**ATTENTION: WORK IN PROGRESS, UNPUBLISHED, NOT PRODUCTION READY**

Backbone is a durable publish-subscribe platform built on top of Amazon AWS SQS and Amazon AWS SNS utilizing
Akka to stream events.

Backbone provides nice high-level API for consuming events.

```scala
backbone.consume[String](settings){ str =>
    println(str)
    Consumed //or Rejected in case of an failure
}
```

## Quick Start

To use backbone add the following dependency to your `build.sbt`.
```scala
libraryDependencies += "com.firstbird" %% "backbone-core" % "0.4.1"
```

Or add the dependency to your `pom.xml`.
```xml
<dependency>
    <groupId>com.firstbird</groupId>
    <artifactId>backbone-core_2.11</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

```scala
import backbone.scaladsl.Backbone

implicit val sns = new AmazonSNSAsyncClient()
implicit val sqs = new AmazonSQSAsyncClient()

val backbone = Backbone()

val settings = ConsumerSettings(
    events = "event-type" :: Nil,
    topics = "arn:aws:sns:eu-central-1:AWS_ACCOUNT_ID:topic-name" :: Nil,
    queue = "queue-name",
    parallelism = 5
)

backbone.consume[String](settings){ str =>
    println(str)
    Consumed
}
```

## Running the Tests

Run the tests from sbt with:
```
test
```

## Contributors

* Pedro Dias
* Fabian Grutsch
* Georgi Lichev
* [Daniel Pfeiffer](https://github.com/dpfeiffer)
