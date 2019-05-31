package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.SdkTest

class AssetsTest extends SdkTest {
  private val client = new Client()

  it should "be possible to retrieve an asset" in {
    val assets = client.assets.read()
    println(assets.unsafeBody.items.headOption.map(_.name).getOrElse("error")) // scalastyle:ignore
  }

  it should "list all assets" in {
    val assets = client.assets.readAll()
    val f = assets.toSeq
    println(f.map(_.length).mkString(", ")) // scalastyle:ignore
    println(f.flatten.length) // scalastyle:ignore
  }

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  private val createdAssets = scala.collection.mutable.ListBuffer[Asset]()
  it should "be possible to create an asset" in {
    val assets = client.assets.create(Seq(Asset(name = "fusion-scala-sdk3")))
    println("wrote assets: ") // scalastyle:ignore
    (createdAssets ++= assets.unsafeBody).foreach(_ => ())
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }

  it should "be possible to create a CreateAsset" in {
    val assets = client.assets.create(Seq(CreateAsset(name = "fusion-scala-sdk2")))
    println("wrote assets: ") // scalastyle:ignore
    (createdAssets ++= assets.unsafeBody).foreach(_ => ())
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }

  it should "be possible to delete an asset" in {
    val r = client.assets.retrieveByIds(createdAssets.flatMap(_.id.toList)).unsafeBody
    assert(r.right.toOption.map(_.size).contains(createdAssets.size))
    println(s"deleting ${createdAssets.length} assets") // scalastyle:ignore
    client.assets.deleteByIds(createdAssets.flatMap(_.id.toList)).unsafeBody
    val r1 = client.assets.retrieveByIds(createdAssets.flatMap(_.id.toList)).unsafeBody
    assert(r1.left.toOption.map(_.size).contains(2))
  }
}
