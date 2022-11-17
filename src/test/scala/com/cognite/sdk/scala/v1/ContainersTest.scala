// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.containers.ContainerPropertyType._
import com.cognite.sdk.scala.v1.containers._
import com.cognite.sdk.scala.v1.resources.Containers
import com.cognite.sdk.scala.v1.resources.Containers.{containerPropertyDefinitionDecoder, containerPropertyDefinitionEncoder}
import io.circe.{Decoder, Encoder}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal"
  )
)
// scalastyle:off
class ContainersTest extends CommonDataModelTestHelper with RetryWhile {
  private val client = new GenericClient[IO](
    "mock-server-test",
    "mock",
    "http://localhost:4002",
    authProvider,
    None,
    None,
    Some("alpha")
  )
  private val containers = new Containers[IO](client.requestSession)

  "Containers" should "serialize & deserialize ConstraintTypes" in {
    val constraintTypes = Seq(ConstraintType.Unique, ConstraintType.Required)

    val encodedAndDecoded = constraintTypes
      .map(Encoder[ConstraintType].apply(_))
      .map(Decoder[ConstraintType].decodeJson)
      .collect { case Right(e) => e }

    constraintTypes should contain theSameElementsAs encodedAndDecoded
  }

  it should "serialize & deserialize ContainerUsage" in {
    val values = Seq(ContainerUsage.Node, ContainerUsage.All)

    val afterEncodedAndDecoded = values
      .map(Encoder[ContainerUsage].apply)
      .map(Decoder[ContainerUsage].decodeJson)
      .collect { case Right(e) => e }

    values should contain theSameElementsAs afterEncodedAndDecoded
  }

  it should "serialize & deserialize PrimitivePropType" in {
    val values = Seq(
      PrimitivePropType.Int32,
      PrimitivePropType.Int64,
      PrimitivePropType.Json,
      PrimitivePropType.Boolean,
      PrimitivePropType.Float32,
      PrimitivePropType.Float64,
      PrimitivePropType.Numeric,
      PrimitivePropType.Timestamp,
      PrimitivePropType.Date
    )

    val afterEncodedAndDecoded = values
      .map(Encoder[PrimitivePropType].apply)
      .map(Decoder[PrimitivePropType].decodeJson)
      .collect { case Right(e) => e }

    values should contain theSameElementsAs afterEncodedAndDecoded
  }

  it should "serialize & deserialize PropertyDefaultValue" in {
    val values = Seq(
      PropertyDefaultValue.String("abc"),
      PropertyDefaultValue.Boolean(true),
      PropertyDefaultValue.Number(123),
      PropertyDefaultValue.Object(Encoder[CogniteExternalId].apply(CogniteExternalId("test-ext-id")))
    )

    val afterEncodedAndDecoded = values
      .map(Encoder[PropertyDefaultValue].apply)
      .map(Decoder[PropertyDefaultValue].decodeJson)
      .collect { case Right(e) => e }

    values should contain theSameElementsAs afterEncodedAndDecoded
  }

  it should "serialize & deserialize ContainerPropertyType" in {
    val values = Seq(
      ContainerPropertyType.TextProperty(),
      ContainerPropertyType.DirectNodeRelationProperty(None),
      ContainerPropertyType.PrimitiveProperty(`type` = PrimitivePropType.Int32)
    )

    val afterEncodedAndDecoded = values
      .map(Encoder[ContainerPropertyType].apply)
      .map(Decoder[ContainerPropertyType].decodeJson)
      .collect { case Right(e) => e }

    values should contain theSameElementsAs afterEncodedAndDecoded
  }

  it should "serialize & deserialize ContainerPropertyDefinition" in {
    val containerProperty = ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Number(1.0)),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32)
    )

    val jsonStr = Encoder[ContainerPropertyDefinition].apply(containerProperty).noSpaces
    val decodedContainerProperty = io.circe.parser.parse(jsonStr).flatMap(Decoder[ContainerPropertyDefinition].decodeJson)

    Some(containerProperty) shouldBe decodedContainerProperty.toOption
  }

  // TODO: fix after the real impl is live
  ignore should "list containers" in {
    val c = containers.list().unsafeRunSync()
    c.length shouldBe 1
  }

  // TODO: fix after the real impl is live
  ignore should "create a container" in {
    val containerProperty = ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Number(1.0)),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32)
    )
    val constraintProperty = ContainerConstraint.UniquenessConstraint(properties = Seq.empty)
    val containerToCreate = ContainerCreate(
      space = "test-space",
      externalId = "test-space-external-id",
      name = Some("test-space-name"),
      description = Some("this is a test space for scala sdk"),
      usedFor = Some(ContainerUsage.All),
      properties = Map("property-1" -> containerProperty),
      constraints = Some(Map("constraint-property-1" -> constraintProperty)),
      indexes = None
    )

    val response = containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()

    response.headOption.isEmpty shouldBe false
  }
}
