// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.containers

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.fdm.Utils._
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.PrimitiveProperty
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyDefinition, PropertyType}
import com.cognite.sdk.scala.v1.{CogniteExternalId, CommonDataModelTestHelper}
import io.circe.{Decoder, Encoder}

import scala.concurrent.duration.DurationInt

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.OptionPartial"
  )
)
class ContainersTest extends CommonDataModelTestHelper {
  private val space = "test-space-scala-sdk"

  "Containers" should "serialize & deserialize ConstraintTypes" in {
    val constraintTypes = Seq(ConstraintType.Unique, ConstraintType.Required)

    val encodedAndDecoded = constraintTypes
      .map(Encoder[ConstraintType].apply(_))
      .map(Decoder[ConstraintType].decodeJson)
      .collect { case Right(e) => e }

    constraintTypes should contain theSameElementsAs encodedAndDecoded
  }

  it should "serialize & deserialize ContainerUsage" in {
    val values = Seq(Usage.Node, Usage.Edge, Usage.All)

    val afterEncodedAndDecoded = values
      .map(Encoder[Usage].apply)
      .map(Decoder[Usage].decodeJson)
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
      PropertyDefaultValue.Int32(101),
      PropertyDefaultValue.Int64(Long.MaxValue),
      PropertyDefaultValue.Float32(101.1F),
      PropertyDefaultValue.Float64(Double.MaxValue),
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
      PropertyType.TextProperty(list = None),
      PropertyType.TextProperty(list = Some(true)),
      PropertyType.TextProperty(list = Some(false)),
      PropertyType.DirectNodeRelationProperty(None),
      PropertyType.DirectNodeRelationProperty(Some(ContainerReference(space, "ext-id-1"))),
      PropertyType.PrimitiveProperty(`type` = PrimitivePropType.Int32, list = None),
      PropertyType.PrimitiveProperty(`type` = PrimitivePropType.Int64, list = Some(true)),
      PropertyType.PrimitiveProperty(`type` = PrimitivePropType.Date, list = Some(false))
    )

    val afterEncodedAndDecoded = values
      .map(Encoder[PropertyType].apply)
      .map(Decoder[PropertyType].decodeJson)
      .collect { case Right(e) => e }

    values should contain theSameElementsAs afterEncodedAndDecoded
  }

  it should "serialize & deserialize ContainerPropertyDefinition" in {
    val containerProperty = ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Float32(1.0F)),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Float32)
    )

    val jsonStr = Encoder[ContainerPropertyDefinition].apply(containerProperty).noSpaces
    val decodedContainerProperty = io.circe.parser.parse(jsonStr).flatMap(Decoder[ContainerPropertyDefinition].decodeJson)

    Some(containerProperty) shouldBe decodedContainerProperty.toOption
  }

  it should "correctly deserialize default values based on propertyType of ContainerPropertyDefinition" in {
    val json = s"""
       |[
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "defaultValue": 1.1,
       |    "description": "Test float32 property",
       |    "name": "numeric-property-prop-1",
       |    "type": {
       |      "type": "float32",
       |      "list": false
       |    }
       |  },
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "defaultValue": 1.2,
       |    "description": "Test float64 property",
       |    "name": "numeric-property-prop-2",
       |    "type": {
       |      "type": "float64",
       |      "list": false
       |    }
       |  },
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "defaultValue": 1,
       |    "description": "Test int32 property",
       |    "name": "numeric-property-prop-3",
       |    "type": {
       |      "type": "int32",
       |      "list": false
       |    }
       |  },
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "defaultValue": 2,
       |    "description": "Test int64 property",
       |    "name": "numeric-property-prop-4",
       |    "type": {
       |      "type": "int64",
       |      "list": false
       |    }
       |  },
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "description": "Test int64 property",
       |    "name": "numeric-property-prop-5",
       |    "type": {
       |      "type": "int64",
       |      "list": false
       |    }
       |  },
       |  {
       |    "nullable": true,
       |    "autoIncrement": false,
       |    "description": "Test int32 property",
       |    "name": "numeric-property-prop-6",
       |    "type": {
       |      "type": "int32",
       |      "list": false
       |    }
       |  }
       |]
       |""".stripMargin

    val propTypesAndDefaultValues = io.circe.parser.parse(json)
      .flatMap(Decoder[List[ContainerPropertyDefinition]].decodeJson)
      .toOption.getOrElse(List.empty)
      .map(p => (p.`type`, p.defaultValue))

    propTypesAndDefaultValues.length shouldBe 6
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Float32), Some(PropertyDefaultValue.Float32(1.1F)))
    ) shouldBe true
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Float64), Some(PropertyDefaultValue.Float64(1.2)))
    ) shouldBe true
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Int32), Some(PropertyDefaultValue.Int32(1)))
    ) shouldBe true
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Int32), None)
    ) shouldBe true
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Int64), Some(PropertyDefaultValue.Int64(2L)))
    ) shouldBe true
    propTypesAndDefaultValues.contains(
      (PropertyType.PrimitiveProperty(PrimitivePropType.Int64), None)
    ) shouldBe true
  }

  it should "validate compatibility with default value type and property type" in {
    an[java.lang.AssertionError] should be thrownBy {
      ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Float32(1.0F)),
        description = None,
        name = None,
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Float64)
      )
    }
    an[java.lang.AssertionError] should be thrownBy {
      ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Int32(1)),
        description = None,
        name = None,
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int64)
      )
    }
  }

  it should "asses the compatibility of default values and property types" in {
    val (compatibles, _) = (for {
      p <- AllContainerPropertyTypes
      d <- AllPropertyDefaultValues
    } yield (p, d, PropertyDefinition.defaultValueCompatibleWithPropertyType(p, d)))
      .partition { case (_, _, compatibility) => compatibility }

    // default values for list types are not allowed
    compatibles.count { case (propType, _, _) => propType.isList } shouldBe 0
    compatibles.length shouldBe 9
  }

  it should "CRUD a container with all possible props" in {
    val containerExternalId = s"test_container_2"
    val allPossibleProperties: Map[String, ContainerPropertyDefinition] = createAllPossibleContainerPropCombinations
    val allPossiblePropertyKeys = allPossibleProperties.keys.toList

    val constraints: Map[String, ContainerConstraint] = Map(
      "uniqueConstraint" -> ContainerConstraint.UniquenessConstraint(
        allPossiblePropertyKeys.take(5)
      )
    )


    val indexes: Map[String, IndexDefinition] = Map(
      "index1" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.take(2)),
      "index2" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.slice(5, 7))
    )

    val containerToCreate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"Test-Container-Name"),
      description = Some("Test Container Description"),
      usedFor = Some(Usage.All),
      properties = allPossibleProperties,
      constraints = Some(constraints),
      indexes = Some(indexes)
    )

    val createdResponse = blueFieldClient.containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()
    createdResponse.isEmpty shouldBe false

    // TODO: Check update reflection delay and remove 10.seconds sleep
    val readAfterCreateContainers = (IO.sleep(10.seconds) *> blueFieldClient
      .containers
      .retrieveByExternalIds(Seq(ContainerId(space, containerExternalId))))
      .unsafeRunSync()
    val insertedContainer = readAfterCreateContainers.find(_.externalId === containerExternalId)

    insertedContainer.isEmpty shouldBe false

//    insertedContainer.get.properties.keys.toList should contain theSameElementsAs allPossibleProperties.keys.toList
//    insertedContainer.get.properties.values.toList should contain theSameElementsAs allPossibleProperties.values.toList

    val allPossiblePropertiesToUpdate = allPossibleProperties.map {
      case (k, v) =>
        k -> (v.`type` match {
          case PropertyType.DirectNodeRelationProperty(_) =>
            v.copy(
              defaultValue = None,
              nullable = Some(true),
              description = v.description.map(d => s"$d Updated"),
              name = v.name.map(n => s"$n-Updated")
            )
          case _ =>
            v.copy(
              defaultValue = None,
              nullable = Some(false),
              description = v.description.map(d => s"$d Updated"),
              name = v.name.map(n => s"$n-Updated")
            )
        })
    }
    val containerToUpdate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"Test-Container-Name-Updated"),
      description = Some("Test Container Description Updated"),
      usedFor = Some(Usage.All),
      properties = allPossiblePropertiesToUpdate,
      constraints = Some(constraints),
      indexes = Some(indexes)
    )

    val updatedResponse = blueFieldClient.containers.createItems(containers = Seq(containerToUpdate)).unsafeRunSync()
    updatedResponse.isEmpty shouldBe false

    // TODO: Check update reflection delay and remove 10.seconds sleep
    val readAfterUpdateContainers = (IO.sleep(10.seconds) *> blueFieldClient
      .containers
      .retrieveByExternalIds(Seq(ContainerId(space, containerExternalId))))
      .unsafeRunSync()
    val updatedContainer = readAfterUpdateContainers.find(_.externalId === containerExternalId).get

    updatedContainer.properties.keys.toList should contain theSameElementsAs allPossiblePropertiesToUpdate.keys.toList
//    updatedContainer.properties.values.toList should contain theSameElementsAs allPossiblePropertiesToUpdate.values.toList
    updatedContainer.name.get.endsWith("Updated") shouldBe true
    updatedContainer.description.get.endsWith("Updated") shouldBe true

    val deletedContainer = blueFieldClient.containers.delete(Seq(ContainerId(space, containerExternalId))).unsafeRunSync()
    deletedContainer.length shouldBe 1
  }
}
