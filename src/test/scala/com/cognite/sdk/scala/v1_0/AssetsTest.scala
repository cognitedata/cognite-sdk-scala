package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class AssetsTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.assets)
  it should behave like writableResource(
      client.assets,
      Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
      Seq(CreateAsset(name = "scala-sdk-create-example-1"), CreateAsset(name = "scala-sdk-create-example-2"))
  )
}
