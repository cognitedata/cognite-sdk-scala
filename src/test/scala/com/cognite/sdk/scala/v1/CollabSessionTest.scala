package com.cognite.sdk.scala.v1

import cats.Id
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, GzipSttpBackend, LoggingSttpBackend, RetryingBackend}
import com.cognite.sdk.scala.v1.resources.Assets
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}
import fs2.Stream
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class CollabSessionTest extends FlatSpec with Matchers {

  private lazy val apiKey = Option(System.getenv("TEST_API_KEY"))
    .getOrElse(throw new RuntimeException("TEST_API_KEY not set"))
  implicit lazy val auth: Auth = ApiKeyAuth(apiKey)

  val sttpBackend: SttpBackend[Id, Nothing] =
    new RetryingBackend[Id, Nothing](
      new GzipSttpBackend[Id, Nothing](
        HttpURLConnectionBackend()
      ),
      initialRetryDelay = 100.millis,
      maxRetryDelay = 200.millis
    )
  val loggingBackend = new LoggingSttpBackend[Id, Nothing](sttpBackend)

  val client: GenericClient[Id] = GenericClient.forAuth[Id](
    "scala-sdk-test", auth)(implicitly, loggingBackend)

  "the test code" should "run the sdk client!" in {
    val assets: Assets[Id] = client.assets
    val assetListingStream: fs2.Stream[Id, Asset] = assets.filter(
      AssetsFilter(
        parentIds = Some(Seq(4292780120595229L))
      )
    )
    val compiledStream: Stream.CompileOps[Id, Id, Asset] = assetListingStream.compile
    val assetsList: Id[List[Asset]] = compiledStream.toList

    assetsList.take(5).foreach(println)

    assetsList.length should be(50)
  }
}
