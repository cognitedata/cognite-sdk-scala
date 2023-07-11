package com.cognite.sdk.scala.common

import cats.effect.IO
import com.cognite.sdk.scala.v1.RequestSession
import sttp.model.Uri

class ReadableTest extends SdkTestSpec {
  it should "not overflow on very long cursor chains" in {
    val readable = new Readable[Int, IO] {
      override val requestSession: RequestSession[IO] = client.requestSession
      override val baseUrl: Uri = client.uri

      override private[sdk] def readWithCursor(cursor: Option[String], limit: Option[Int], partition: Option[Partition]): IO[ItemsWithCursor[Int]] = {
        val nextCursor = cursor.map(_.toInt + 1).getOrElse(0).toString
        val itemsToReturn = limit.map(math.min(_, 1000)).getOrElse(1000)
        IO.pure(ItemsWithCursor(List.fill(itemsToReturn)(0), Some(nextCursor)))
      }
    }

    val limit = 50000000 /*50M*/;
    val longList = readable.list(Some(limit)).compile.toList.unsafeRunSync()
    longList should be(List.fill(limit)(0))
  }
}
