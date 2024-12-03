// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.Client
import io.circe.{Codec, Decoder, Encoder}
import sttp.client3._
import sttp.client3.testing.SttpBackendStub
import io.circe.parser.decode
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import org.scalatest.OptionValues
import sttp.model.StatusCode
import sttp.monad.EitherMonad

final case class DummyFilter()
object DummyFilter {
  implicit val dummyFilterCodec: Codec[DummyFilter] = deriveCodec[DummyFilter]
}

class FilterTest extends SdkTestSpec with OptionValues {
  private implicit val dummyFilterRequestEncoder: Encoder[FilterRequest[DummyFilter]] = deriveEncoder[FilterRequest[DummyFilter]]
  private implicit val dummyFilterRequestDecoder: Decoder[FilterRequest[DummyFilter]] = deriveDecoder[FilterRequest[DummyFilter]]
  private implicit val dummyItemsWithCursorDecoder: Decoder[ItemsWithCursor[Int]] = deriveDecoder[ItemsWithCursor[Int]]

  it should "set final limit to batchSize when less than limit" in filterWithCursor(10, Some(100)) { finalLimit =>
    finalLimit should be(10)
  }

  it should "set final limit to limit when less than batchsize" in filterWithCursor(100, Some(20)) { finalLimit =>
    finalLimit should be(20)
  }

  it should "set final limit to batchsize when no limit" in filterWithCursor(100, None) { finalLimit =>
    finalLimit should be(100)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var", "org.wartremover.warts.AsInstanceOf"))
  def filterWithCursor(batchSize: Int, limit: Option[Int])(test: Int => Any): Any = {
    var hijackedRequest: FilterRequest[DummyFilter] = null
    val requestHijacker = SttpBackendStub(EitherMonad)
      .whenAnyRequest.thenRespondF(req => {
        for {
          req <- decode[FilterRequest[DummyFilter]](req.body.asInstanceOf[StringBody].s)
          _ = { hijackedRequest = req }
        } yield Response(Right(ItemsWithCursor(Seq(0, 1, 2), None)), StatusCode.Ok, "OK")
        
    })
    lazy val dummyClient = Client("foo",
      projectName,
      "https://api.cognitedata.com",
      auth)(implicitly, requestHijacker)
    val dummyRequestSession = dummyClient.requestSession

    val _ = Filter.filterWithCursor(
      dummyRequestSession,
      uri"https://test.com",
      DummyFilter(),
      None,
      limit,
      None,
      batchSize,
      None
    )
    test(hijackedRequest.limit.value)
  }
}
