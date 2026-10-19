// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.OAuth2.ClientCredentials
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.model.Uri

class TransformationsCredentialsEncoderTest extends AnyFlatSpec with Matchers {
  import FlatOidcCredentials.credentialEncoder

  "ClientCredentials encoder" should "work with empty scopes and empty audience" in {
    val credential = ClientCredentials(
      Uri.unsafeParse("http://tokenUrl.com"),
      "gcp",
      "secret",
      List.empty[String],
      "project",
      None
    ).asJson
    credential.toString() shouldBe """{
                                     |  "clientId" : "gcp",
                                     |  "clientSecret" : "secret",
                                     |  "tokenUri" : "http://tokenUrl.com",
                                     |  "cdfProjectName" : "project"
                                     |}""".stripMargin
  }

  it should "work with non empty scopes and non empty audience" in {
    val credential = ClientCredentials(
      Uri.unsafeParse("http://tokenUrl.com"),
      "gcp",
      "secret",
      (1 to 3).map(i => s"scope-${i.toString}").toList,
      "project",
      Some("audience")
    ).asJson
    credential.toString() shouldBe """{
                                     |  "clientId" : "gcp",
                                     |  "clientSecret" : "secret",
                                     |  "tokenUri" : "http://tokenUrl.com",
                                     |  "cdfProjectName" : "project",
                                     |  "audience" : "audience",
                                     |  "scopes" : "scope-1 scope-2 scope-3"
                                     |}""".stripMargin
  }
}
