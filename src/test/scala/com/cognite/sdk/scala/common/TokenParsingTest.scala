package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.Capability
import io.circe.literal._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TokenParsingTest extends AnyFlatSpec with Matchers {
  it should "decode AllProjectsScope" in {
    val r = ProjectScope.decoder.decodeJson(json"""
      {
        "allProjects": {}
      }
    """)
    r shouldBe Right(AllProjectsScope())
  }
  it should "encode AllProjectsScope" in {
    val r = ProjectScope.decoder.decodeJson(
      ProjectScope.encoder.apply(AllProjectsScope())
    )
    r shouldBe Right(AllProjectsScope())
  }
  it should "decode ProjectsListScope" in {
    val r = ProjectScope.decoder.decodeJson(json"""
      {
        "projects": ["a"]
      }
    """)
    r shouldBe Right(ProjectsListScope(Seq("a")))
  }
  it should "encode ProjectsListScope" in {
    val r = ProjectScope.decoder.decodeJson(
      ProjectScope.encoder.apply(ProjectsListScope(Seq("a")))
    )
    r shouldBe Right(ProjectsListScope(Seq("a")))
  }
  it should "require *Acl field" in {
    val r = ProjectCapability.decoder.decodeJson(json"""
      {
        "projectScope": {"allProjects": {}}
      }
    """)
    r.isLeft shouldBe true
  }
  it should "decode allProjects capability" in {
    val r = ProjectCapability.decoder.decodeJson(json"""
      {
        "AssetsAcl": {"actions": [], "scope": {}},
        "projectScope": {"allProjects": {}}
      }
    """)
    r shouldBe Right(ProjectCapability(
      resourceAcl = Map("AssetsAcl" -> Capability(Seq(), Map())),
      projectScope = AllProjectsScope()
    ))
  }
  it should "decode projectsList capability" in {
    val r = ProjectCapability.decoder.decodeJson(json"""
      {
          "AssetsAcl": {"actions": [], "scope": {}},
          "projectScope": {"projects": ["a", "b"]}
      }
    """)
    r shouldBe Right(ProjectCapability(
      resourceAcl = Map("AssetsAcl" -> Capability(Seq(), Map())),
      projectScope = ProjectsListScope(
        Seq("a", "b")
      )
    ))
  }
  it should "decode short inspect() result" in {
    val r = TokenInspectResponse.decoder.decodeJson(json"""
      {
        "subject": "s",
        "projects": [],
        "capabilities": []
      }
    """)
    r shouldBe Right(TokenInspectResponse("s", Seq(), Seq()))
  }
  it should "decode full inspect() result" in {
    val r = TokenInspectResponse.decoder.decodeJson(json"""
      {
        "subject": "s",
        "projects": [
          {"projectUrlName": "a", "groups": [1]},
          {"projectUrlName": "b", "groups": [2]}
        ],
        "capabilities": [
          {
            "assetsAcl": {
              "actions": ["READ"],
              "scope": {"all": {}}
            },
            "projectScope": {
              "projects": ["b"]
            }
          }
        ]
      }
    """)
    r shouldBe Right(TokenInspectResponse(
      subject = "s",
      projects = Seq(
        ProjectDetails("a", Seq(1)), ProjectDetails("b", Seq(2))
      ),
      capabilities = Seq(ProjectCapability(
        resourceAcl = Map("assetsAcl" -> Capability(
          actions = Seq("READ"),
          scope = Map("all" -> Map.empty))
        ),
        projectScope = ProjectsListScope(Seq("b"))
      ))
    ))
  }
  it should "encode full inspect() result" in {
    val value = TokenInspectResponse(
      subject = "s",
      projects = Seq(
        ProjectDetails("a", Seq(1)), ProjectDetails("b", Seq(2))
      ),
      capabilities = Seq(ProjectCapability(
        resourceAcl = Map("assetsAcl" -> Capability(
          actions = Seq("READ"),
          scope = Map("all" -> Map.empty))
        ),
        projectScope = ProjectsListScope(Seq("b"))
      ))
    )
    val r = TokenInspectResponse.decoder.decodeJson(
      TokenInspectResponse.encoder.apply(value)
    )
    r shouldBe Right(value)
  }
}
