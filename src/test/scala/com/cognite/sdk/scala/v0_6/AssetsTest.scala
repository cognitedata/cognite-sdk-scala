package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class AssetsTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.assets, supportsMissingAndThrown = false)
  it should behave like writableResource(
    client.assets,
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(CreateAsset(name = "scala-sdk-create-example-1"), CreateAsset(name = "scala-sdk-create-example-2")),
    supportsMissingAndThrown = false
  )
}
