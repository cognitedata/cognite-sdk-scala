package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.SdkTest

class AssetsTest extends SdkTest {
  it should "be possible to retrieve an asset" in {
    val client = new Client()
    val assets = client.assets.read()
    println(assets.unsafeBody.items.headOption.map(_.name).getOrElse("error")) // scalastyle:ignore
  }

  it should "fetch all assets" in {
    val client = new Client()
    val assets = client.assets.readAll()
    val f = assets.toSeq
    println(f.map(_.length).mkString(", ")) // scalastyle:ignore
    println(f.flatten.length) // scalastyle:ignore
  }

  it should "be possible to write an asset" in {
    val client = new Client()
    val assets = client.assets.write(Seq(Asset(name = "fusion-scala-sdk3")))
    println("wrote assets: ") // scalastyle:ignore
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }

  it should "be possible to write a postasset" in {
    val client = new Client()
    val assets = client.assets.write(Seq(PostAsset(name = "fusion-scala-sdk2")))
    println("wrote assets: ") // scalastyle:ignore
    println(assets.unsafeBody.map(_.toString).mkString(", ")) // scalastyle:ignore
  }
}
