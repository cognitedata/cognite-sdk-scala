// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.Client
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import io.circe.generic.semiauto.deriveDecoder
import sttp.model.Uri.QuerySegment

class ReadTest extends SdkTestSpec {
  implicit val dummyItemsWithCursorDecoder = deriveDecoder[ItemsWithCursor[Int]]
  it should "set final limit to batchSize when less than limit" in readWithCursor(10, Some(100)) { finalLimit =>
    finalLimit should be(10)
  }

  it should "set final limit to limit when less than batchsize" in readWithCursor(100, Some(20)) { finalLimit =>
    finalLimit should be(20)
  }

  it should "set final limit to batchsize when no limit" in readWithCursor(100, None) { finalLimit =>
    finalLimit should be(100)
  }

  def readWithCursor(batchSize: Int, limit: Option[Int])(test: Int => Any): Any = {
    var totalLimit = 0
    val requestHijacker = SttpBackendStub.synchronous.whenAnyRequest.thenRespondF(req => {
      totalLimit = req.uri.querySegments.collectFirst {
        case q @ QuerySegment.KeyValue("limit", _, _, _) => q.v.toInt
      }.get
      Response.ok(0)
    })
    lazy val dummyClient = Client("foo",
      projectName,
      "https://api.cognitedata.com",
      auth)(requestHijacker)
    val dummyRequestSession = dummyClient.requestSession

    Readable.readWithCursor(
      dummyRequestSession,
      uri"https://test.com",
      None,
      limit,
      None,
      batchSize
    )
    test(totalLimit)
  }
}
