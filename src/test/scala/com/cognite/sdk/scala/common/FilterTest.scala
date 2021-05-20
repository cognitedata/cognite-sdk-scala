// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.Client
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import io.circe.parser.decode
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.model.StatusCode

case class DummyFilter()

class FilterTest extends SdkTestSpec {
  implicit val dummyFilterEncoder = deriveEncoder[DummyFilter]
  implicit val dummyFilterRequestEncoder = deriveEncoder[FilterRequest[DummyFilter]]
  implicit val dummyFilterDecoder = deriveDecoder[DummyFilter]
  implicit val dummyFilterRequestDecoder = deriveDecoder[FilterRequest[DummyFilter]]
  implicit val dummyItemsWithCursorDecoder = deriveDecoder[ItemsWithCursor[Int]]

  it should "set final limit to batchSize when less than limit" in filterWithCursor(10, Some(100)) { finalLimit =>
    finalLimit should be(10)
  }

  it should "set final limit to limit when less than batchsize" in filterWithCursor(100, Some(20)) { finalLimit =>
    finalLimit should be(20)
  }

  it should "set final limit to batchsize when no limit" in filterWithCursor(100, None) { finalLimit =>
    finalLimit should be(100)
  }

  def filterWithCursor(batchSize: Int, limit: Option[Int])(test: Int => Any): Any = {
    var hijackedRequest: FilterRequest[DummyFilter] = null // scalastyle:ignore
    val requestHijacker = SttpBackendStub.synchronous.whenAnyRequest.thenRespondF(req => {
      hijackedRequest = decode[FilterRequest[DummyFilter]](req.body.asInstanceOf[StringBody].s) match {
        case Right(x) => x
        case Left(e) => throw e
      }
      Response(ItemsWithCursor(Seq(0, 1, 2), None), StatusCode.Ok, "OK")
    })
    lazy val dummyClient = Client("foo",
      projectName,
      "https://api.cognitedata.com",
      auth)(requestHijacker)
    val dummyRequestSession = dummyClient.requestSession

    Filter.filterWithCursor(
      dummyRequestSession,
      uri"https://test.com",
      DummyFilter(),
      None,
      limit,
      None,
      batchSize,
      None
    )
    test(hijackedRequest.limit.get)
  }
}
