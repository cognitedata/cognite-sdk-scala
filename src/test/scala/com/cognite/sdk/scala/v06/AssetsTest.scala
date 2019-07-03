package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import com.cognite.sdk.scala.v06.resources.{Asset, CreateAsset}

class AssetsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.assets)
  it should behave like readableWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = false)
  it should behave like writable(
    client.assets,
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(CreateAsset(name = "scala-sdk-create-example-1"), CreateAsset(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )
}
