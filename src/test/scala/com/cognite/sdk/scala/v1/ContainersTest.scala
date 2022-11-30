// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.containers.ContainerPropertyType._
import com.cognite.sdk.scala.v1.containers._
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
  private val space = "test-space-scala-sdk"
//  private val containerName = "test-container-scala-sdk"
//  private val spaceDefinition = blueFieldClient.spacesv3.retrieveItems(Seq(SpaceById(space)))
//    .map(_.headOption)
//    .unsafeRunSync()
//  private val containerDefinition = blueFieldClient.containers.retrieveByExternalIds(Seq(ContainerId(space, externalId = containerName)))
//    .map(_.headOption)
//    .unsafeRunSync()

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
      PropertyDefaultValue.Integer(123),
      PropertyDefaultValue.Double(123.45),
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
      defaultValue = Some(PropertyDefaultValue.Double(1.0)),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Float64)
    )

    val jsonStr = Encoder[ContainerPropertyDefinition].apply(containerProperty).noSpaces
    val decodedContainerProperty = io.circe.parser.parse(jsonStr).flatMap(Decoder[ContainerPropertyDefinition].decodeJson)

    Some(containerProperty) shouldBe decodedContainerProperty.toOption
  }


  // TODO: fix after the real impl is live
  ignore should "list containers" in {
    val c = blueFieldClient.containers.list().unsafeRunSync()
    c.length shouldBe 1
  }


  it should "create a container" in {
    val vehicleContainerProperties = Map[String, ContainerPropertyDefinition](
      "manufacturer" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle manufacturer"),
        name = Some("vehicle-manufacturer-name"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "model" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle model"),
        name = Some("vehicle-model-name"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "year" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle manufactured year"),
        name = Some("vehicle-manufactured-year"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32),
        nullable = Some(false)
      ),
      "displacement" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle engine displacement"),
        name = Some("vehicle-engine-displacement"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32),
        nullable = Some(false)
      ),
      "weight" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle weight in Kg"),
        name = Some("vehicle-weight"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int64),
        nullable = Some(false)
      ),
      "break-type" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.String("ABS")),
        description = Some("vehicle break type"),
        name = Some("vehicle-break-type"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
        nullable = Some(false)
      ),
      "cylinders" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Integer(4)),
        description = Some("number of cylinders"),
        name = Some("number-of-cylinders"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32),
        nullable = Some(true)
      ),
      "compression-ratio" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.String("17.5:1")),
        description = Some("engine compression ratio"),
        name = Some("compression-ratio"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "turbocharger" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Boolean(false)),
        description = Some("turbocharger availability"),
        name = Some("turbocharger"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
        nullable = Some(false)
      ),
      "vtec" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Boolean(false)),
        description = Some("vtec availability"),
        name = Some("vtec"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
        nullable = Some(true)
      )
    )

    val containerToCreate = ContainerCreate(
      space = space,
      externalId = s"$space-vehicle-container-1",
      name = Some(s"vehicle-container"),
      description = Some("Test container for modeling vehicles"),
      usedFor = Some(ContainerUsage.All),
      properties = vehicleContainerProperties,
      constraints = Some(Map(
        "unique-properties" -> ContainerConstraint.UniquenessConstraint(Seq("manufacturer", "model")))
      ),
      indexes = Some(Map(
        "manufacturer-index" -> IndexDefinition.BTreeIndexDefinition(Seq("manufacturer")),
        "model-index" -> IndexDefinition.BTreeIndexDefinition(Seq("model")))
      )
    )

    val response = blueFieldClient.containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()

    response.headOption.isEmpty shouldBe false
  }
}
