// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.sttp

import io.circe.Json
import io.circe.parser.{decode, parse}
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import RecordedContent._

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class VcrBackendTest extends AnyFlatSpec with Matchers {

  "RecordedContent.fromBytes" should "produce TextContent for small non-JSON bodies" in {
    fromBytes("hello".getBytes("UTF-8"), "text/plain") shouldBe TextContent("hello")
  }

  it should "produce JsonContent for small application/json bodies" in {
    val json = """{"key":"value"}"""
    fromBytes(json.getBytes("UTF-8"), "application/json") shouldBe
      JsonContent(parse(json).toOption.get)
  }

  it should "fall back to TextContent for malformed JSON with application/json content type" in {
    fromBytes("not json".getBytes("UTF-8"), "application/json") shouldBe TextContent("not json")
  }

  it should "produce GzippedContent for bodies at or above the 1000-byte threshold" in {
    fromBytes(Array.fill(1000)(65.toByte), "text/plain") shouldBe a[GzippedContent]
  }

  it should "not gzip bodies below the threshold" in {
    fromBytes(Array.fill(999)(65.toByte), "text/plain") shouldBe a[TextContent]
  }

  "GzippedContent.toBytes" should "decompress back to the original bytes" in {
    val original = Array.fill(1000)(65.toByte)
    fromBytes(original, "text/plain") match {
      case gz: GzippedContent => gz.toBytes.toSeq shouldBe original.toSeq
      case other              => fail(s"Expected GzippedContent, got $other")
    }
  }

  "TextContent.toBytes" should "return UTF-8 encoded bytes" in {
    TextContent("héllo").toBytes.toSeq shouldBe "héllo".getBytes("UTF-8").toSeq
  }

  "JsonContent.toBytes" should "return compact JSON bytes" in {
    val json = parse("""{"a":1}""").toOption.get
    JsonContent(json).toBytes shouldBe """{"a":1}""".getBytes("UTF-8")
  }

  "HeaderCodecs" should "encode a header list as a JSON array" in {
    import HeaderCodecs._
    val encoded = List("v1", "v2").asJson
    encoded.isArray shouldBe true
    encoded.asArray.get.flatMap(_.asString) shouldBe Vector("v1", "v2")
  }

  it should "decode a JSON array to List[String]" in {
    import HeaderCodecs._
    decode[List[String]]("""["v1","v2"]""").toOption shouldBe Some(List("v1", "v2"))
  }

  "Cassette" should "round-trip through JSON encoding" in {
    import CassetteCodecs._
    val cassette = Cassette(
      interactions = List(
        Interaction(
          RecordedRequest(
            method = "GET",
            uri = "https://example.com/items",
            headers = Map("accept" -> List("application/json")),
            entity = None
          ),
          RecordedResponse(
            method = "GET",
            uri = "https://example.com/items",
            status = RecordedStatus(200, "OK"),
            headers = Map("content-type" -> List("application/json")),
            entity = Some(
              RecordedEntity(
                contentLength = 10L,
                contentType = "application/json",
                contentEncoding = None,
                content = JsonContent(Json.obj("ok" -> Json.True))
              )
            )
          )
        )
      )
    )

    decode[Cassette](cassette.asJson.noSpaces).toOption shouldBe Some(cassette)
  }
}
