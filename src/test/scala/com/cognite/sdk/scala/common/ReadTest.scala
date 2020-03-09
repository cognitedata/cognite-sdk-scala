package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.Client
import com.softwaremill.sttp.Uri.QueryFragment
import com.softwaremill.sttp._
import com.softwaremill.sttp.testing.SttpBackendStub
import io.circe.derivation.deriveDecoder

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
    val requestHijacker = SttpBackendStub.synchronous.whenAnyRequest.thenRespondWrapped(req => {
      totalLimit = req.uri.queryFragments.collectFirst {
        case q @ QueryFragment.KeyValue("limit", _, _, _) => q.v.toInt
      }.get
      Response(Right(0), 200, "OK")
    })
    lazy val dummyClient = Client("foo",
      projectName,
      auth,
      "https://api.cognitedata.com")(requestHijacker)
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
