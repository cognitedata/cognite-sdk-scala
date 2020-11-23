package com.cognite.sdk.scala.common

import org.scalatest.{FlatSpec, Matchers}
import com.softwaremill.sttp._
import io.circe._

class CdpApiExceptionSpec extends FlatSpec with Matchers {
  it should "format messages without request ID" in {
    val ex = CdpApiException(
      url = uri"https://api.cognitedata.com",
      code = 404,
      message = "Not Found",
      missing = None,
      duplicated = None,
      missingFields = None,
      requestId = None
    )

    ex.getMessage shouldBe s"Request to https://api.cognitedata.com failed with status 404: Not Found."
  }

  it should "format messages with request ID" in {
    val ex = CdpApiException(
      url = uri"https://api.cognitedata.com",
      code = 400,
      message = "Bad Request",
      missing = None,
      duplicated = None,
      missingFields = None,
      requestId = Some("1234")
    )

    ex.getMessage shouldBe s"Request with id 1234 to https://api.cognitedata.com failed with status 400: Bad Request."
  }

  it should "format messages with request ID and duplicated" in {
    val ex = CdpApiException(
      url = uri"https://api.cognitedata.com",
      code = 400,
      message = "Bad Request",
      missing = None,
      duplicated = Some(Seq(
        JsonObject("id" -> Json.fromInt(2)),
        JsonObject("id" -> Json.fromInt(1)),
        JsonObject("externalId" -> Json.fromString("externalId-2")),
        JsonObject("externalId" -> Json.fromString("externalId-1"))
      )),
      missingFields = None,
      requestId = Some("1234")
    )

    ex.getMessage should be(
      s"Request with id 1234 to https://api.cognitedata.com failed with status 400: Bad Request. " +
        "Duplicated externalIds: [externalId-1, externalId-2]. Duplicated ids: [1, 2]."
    )
  }

  it should "format messages with request ID and duplicated and missing" in {
    val ex = CdpApiException(
      url = uri"https://api.cognitedata.com",
      code = 400,
      message = "Bad Request",
      missing = Some(Seq(
        JsonObject("id" -> Json.fromInt(3))
      )),
      duplicated = Some(Seq(
        JsonObject("id" -> Json.fromInt(2)),
        JsonObject("externalId" -> Json.fromString("externalId-2")),
        JsonObject("externalId" -> Json.fromString("externalId-1")),
        JsonObject("id" -> Json.fromInt(1))
      )),
      missingFields = None,
      requestId = Some("1234")
    )

    ex.getMessage shouldBe
      s"Request with id 1234 to https://api.cognitedata.com failed with status 400: Bad Request. " +
        "Duplicated externalIds: [externalId-1, externalId-2]. Duplicated ids: [1, 2]. Missing ids: [3]."
  }

  it should "format messages with missing fields" in {
    val ex = CdpApiException(
      url = uri"https://api.cognitedata.com",
      code = 400,
      message = "Bad Request",
      missing = None,
      duplicated = None,
      missingFields = Some(Seq("foo", "bar")),
      requestId = None
    )

    ex.getMessage shouldBe
      s"Request to https://api.cognitedata.com failed with status 400: Bad Request. Missing fields: [foo, bar]."
  }
}
