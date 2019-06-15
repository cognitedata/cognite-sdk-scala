package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class AssetsTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  it should behave like readableResource(client.assets)
  it should behave like readableResourceWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writableResource(
    client.assets,
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(CreateAsset(name = "scala-sdk-create-example-1"), CreateAsset(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
