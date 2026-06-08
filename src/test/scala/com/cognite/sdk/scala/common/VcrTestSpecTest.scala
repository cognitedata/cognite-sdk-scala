// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class VcrTestSpecTest extends AnyFlatSpec with Matchers {

  private def simplify(name: String): String = VcrTestSpec.simplifyFilename(name)

  "VcrTestSpec.simplifyFilename" should "replace spaces and special characters with hyphens" in {
    simplify("foo bar") shouldBe "foo-bar"
    simplify("foo[bar]") shouldBe "foo-bar"
  }

  it should "collapse consecutive hyphens" in {
    simplify("foo  bar") shouldBe "foo-bar"
    simplify("a---b") shouldBe "a-b"
  }

  it should "strip leading and trailing hyphens" in {
    simplify("[foo]") shouldBe "foo"
    simplify(" leading") shouldBe "leading"
  }

  it should "preserve dots and alphanumeric characters" in {
    simplify("foo.bar") shouldBe "foo.bar"
    simplify("FooSpec123") shouldBe "FooSpec123"
  }

  it should "clean up dot-adjacent hyphens" in {
    simplify("foo.-bar") shouldBe "foo.bar"
    simplify("foo-.bar") shouldBe "foo.bar"
  }

  it should "handle a typical ScalaTest test name" in {
    simplify("MySpec should do something useful") shouldBe "MySpec-should-do-something-useful"
  }
}
