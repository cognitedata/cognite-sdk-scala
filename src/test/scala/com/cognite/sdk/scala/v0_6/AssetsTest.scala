package com.cognite.sdk.scala.v0_6

import org.scalatest.{FlatSpec, Matchers}
import com.softwaremill.sttp._

class AssetsTest extends FlatSpec with Matchers {
  val apiKey = System.getenv("COGNITE_API_KEY")
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val auth: Auth = ApiKeyAuth(apiKey)

  it should "be possible to retrieve an asset" in {
    val client = new Client()
    val assets = client.assets.read()
    println(assets.unsafeBody.items.head.name)
  }

  it should "read login status" in {
    val client = new Client()
    val status = client.login.status()
    println(status.unsafeBody)
  }

  it should "fetch all assets" in {
    val client = new Client()
    val assets = client.assets.readAll()
    val f = assets.toSeq
    println(f.map(_.length).mkString(", "))
    println(f.flatten.length)
  }

  it should "be possible to write an asset" in {
    val client = new Client()
    val assets = client.assets.write(Seq(Asset(name = "fusion-scala-sdk3")))
    println("wrote assets: ")
    println(assets.unsafeBody.map(_.toString).mkString(", "))
  }

  it should "be possible to write a postasset" in {
    val client = new Client()
    val assets = client.assets.write(Seq(PostAsset(name = "fusion-scala-sdk2")))
    println("wrote assets: ")
    println(assets.unsafeBody.map(_.toString).mkString(", "))
  }
}
