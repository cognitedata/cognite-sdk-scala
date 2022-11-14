// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.containers.ContainerPropertyType._
import com.cognite.sdk.scala.v1.containers._
import com.cognite.sdk.scala.v1.resources.Containers
import com.cognite.sdk.scala.v1.resources.Containers._
import io.circe.parser.parse
import io.circe.{Decoder, Encoder}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements"
  )
)
class ContainersTest extends CommonDataModelTestHelper with RetryWhile {
  val client = new GenericClient[IO](
    "mock-server-test",
    "mock",
    "http://localhost:4002",
    authProvider,
    None,
    None,
    Some("alpha")
  )
  val containers = new Containers[IO](client.requestSession)

//  it should "list all data models definitions" in {
//    println(Encoder[ConstraintType].apply(ConstraintType.Unique).noSpaces)
//    println(Encoder[ContainerUsage].apply(ContainerUsage.Node).noSpaces)
//    println(Encoder[PrimitivePropType].apply(PrimitivePropType.Int32).noSpaces)
//    println(Encoder[PropertyDefaultValue].apply(PropertyDefaultValue.Object).noSpaces)
//    println(Encoder[ContainerPropertyType].apply(ContainerPropertyType.TextProperty()).noSpaces)
//    println(Encoder[ContainerPropertyType].apply(ContainerPropertyType.DirectNodeRelationProperty(None)).noSpaces)
//    1 shouldBe 1
//  }

  it should "list containers" in {
    val c = containers.list().unsafeRunSync()
    println(c)

    1 shouldBe 1
  }

  ignore should "create a container" in {
    val containerProperty = ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Number),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = Some(PrimitiveProperty(`type` = PrimitivePropType.Int32))
    )
    val constraintProperty = ConstraintDefinition(ConstraintType.Required, properties = Seq.empty)
    val containerToCreate = Container(
      space = "test-space",
      externalId = "test-space-external-id",
      name = Some("test-space-name"),
      description = Some("this is a test space for scala sdk"),
      usedFor = Some(ContainerUsage.All),
      properties = Map("property-1" -> containerProperty),
      constraints = Some(Map("constraint-property-1" -> constraintProperty)),
      indexes = None
    )

    println(Encoder[Container].apply(containerToCreate))

    val response = containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()

    response.headOption.isEmpty shouldBe false
  }

  it should "pass" in {

    val prop1 = Decoder[TextProperty].decodeJson(parse("""{"type": "text","nullable": false}""".stripMargin).toOption.get)
    val prop2 = Decoder[PrimitiveProperty].decodeJson(parse("""{"type": "int32","nullable": false}""".stripMargin).toOption.get)
    val prop3 = Decoder[DirectNodeRelationProperty].decodeJson(parse("""{"type": "direct","nullable": false,"targetModelExternalId": "UserTable"}""".stripMargin).toOption.get)
    val prop4 = Decoder[Map[String, ContainerPropertyType]].decodeJson(parse(s"""{
                                                                   |        "title": {
                                                                   |          "type": "text",
                                                                   |          "nullable": false
                                                                   |        },
                                                                   |        "views": {
                                                                   |          "type": "int32",
                                                                   |          "nullable": false
                                                                   |        },
                                                                   |        "user": {
                                                                   |          "type": "direct",
                                                                   |          "nullable": true,
                                                                   |          "targetModelExternalId": "UserTable"
                                                                   |        }
                                                                   |      }""".stripMargin).toOption.get)

    println(prop1)
    println(prop2)
    println(prop3)
    println(prop4)

    val json =
      s"""
         |{
         |      "space": "blog",
         |      "name": "Post",
         |      "description": "The Container for Post",
         |      "externalId": "PostTable",
         |      "usedFor": "node",
         |      "properties": {
         |        "title": {
         |          "type": "text",
         |          "nullable": false
         |        },
         |        "views": {
         |          "type": "int32",
         |          "nullable": false
         |        },
         |        "user": {
         |          "type": "direct",
         |          "nullable": true,
         |          "targetModelExternalId": "UserTable"
         |        }
         |      }
         |    }
         |""".stripMargin

    val result = Decoder[Container].decodeJson(parse(json).toOption.get)
    println(result)
    1 shouldBe 1
  }
}
