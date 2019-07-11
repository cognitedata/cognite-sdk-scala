package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import io.scalaland.chimney.Transformer

class AssetsTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  implicit val toOptionString: Transformer[String, Option[String]] = new Transformer[String, Option[String]] {
    override def transform(value: String): Option[String] = Some(value)
  }

  it should behave like readable(client.assets)
  it should behave like readableWithRetrieve(client.assets, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writable(
    client.assets,
    Seq(Asset(name = "scala-sdk-read-example-1"), Asset(name = "scala-sdk-read-example-2")),
    Seq(CreateAsset(name = "scala-sdk-create-example-1"), CreateAsset(name = "scala-sdk-create-example-2")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should behave like updatable(
    client.assets,
    Seq(Asset(name = "scala-sdk-update-1"), Asset(name = "scala-sdk-update-2")),
    Seq(Asset(name = "scala-sdk-update-1-1", description = null), Asset(name = "scala-sdk-update-2-1")), // scalastyle:ignore null
    (id: Long, update: Asset) => { update.copy(id = id) },
    (read: Asset, updated: Asset) => {
      s"${read.name}-1" == updated.name
    }
  )
}
