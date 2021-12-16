# Integration Test

## Step 1: Create CFN Stack

Create a new stack in your AWS account using the template: `cfn-templates/backbone-demo.yml`

## Step 2: Setup AWS Credentials

The Cloudformation stack above creates an IAM user with permissions that are required to run Backbone.
Check the stack outputs which contains the region, access key id and secret access key.

On your machine append the credentials to the following file `~/.aws/credentials`:

```
[backbone-demo]
aws_access_key_id=<stack-outputs>
aws_secret_access_key=<stack-outputs>
region=<stack-outputs>
```

## Step 3: Adjust Source Code

Adjust the `topicArn` and `queueName` in `src/main/scala/backbone/it/BackboneCoreDemoApp.scala` with the values you
got from the stack outputs.

Adjust the `topicArn` and `queueUrl` in `src/main/scala/backbone/it/BackboneConsumerPublisherDemoApp.scala` with the
values you got from the stack outputs.

## Step 4: Run Demo App(s)

To run the demo which uses the `backbone-core` module run the following in your terminal:
`sbt integrationtest/runMain backbone.it.BackboneCoreDemoApp`

To run the demo which uses the `backbone-consumer` and `backbone-publisher` modules run the following in your terminal:
`sbt integrationtest/runMain backbone.it.BackboneConsumerPublisherDemoApp`