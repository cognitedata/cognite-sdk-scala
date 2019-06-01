package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.SdkTest

class AssetsTest extends SdkTest {
  private val client = new Client()

  it should "be possible to retrieve an asset" in {
    val assets = client.assets.read()
    println(assets.unsafeBody.items.headOption.map(_.name).getOrElse("error")) // scalastyle:ignore
  }

  it should "fetch all assets" in {
    val assets = client.assets.readAll()
    val f = assets.toSeq
    println(f.map(_.length).mkString(", ")) // scalastyle:ignore
    println(f.flatten.length) // scalastyle:ignore
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var createdAssets = Seq[Asset]()
  it should "be possible to write an asset" in {
    val assets = client.assets.create(Seq(Asset(name = "fusion-scala-sdk3")))
    println("wrote assets: ") // scalastyle:ignore
    createdAssets = assets.unsafeBody
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }

  it should "be possible to write a postasset" in {
    val assets = client.assets.create(Seq(CreateAsset(name = "fusion-scala-sdk2")))
    println("wrote assets: ") // scalastyle:ignore
    createdAssets = createdAssets ++ assets.unsafeBody
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }

  it should "be possible to delete an asset" in {
    val nAssetsBeforeDelete = client.assets.readAll().toSeq.length
    println(s"$nAssetsBeforeDelete before delete") // scalastyle:ignore
    println(s"deleting ${createdAssets.length} assets") // scalastyle:ignore
    client.assets.deleteByIds(createdAssets.flatMap(_.id.toList)).unsafeBody
    Thread.sleep(10000)
    val nAssetsAfterDelete = client.assets.readAll().toSeq.length
    println(s"$nAssetsAfterDelete after delete") // scalastyle:ignore
  }
}
