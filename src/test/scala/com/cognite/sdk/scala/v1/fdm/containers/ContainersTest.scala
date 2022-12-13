// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.containers

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.{PrimitiveProperty, TextProperty}
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.containers.ContainersTest.VehicleContainer._
import com.cognite.sdk.scala.v1.fdm.instances.{EdgeOrNodeData, InstancePropertyValue}
import com.cognite.sdk.scala.v1.{CogniteExternalId, CommonDataModelTestHelper}
import io.circe.{Decoder, Encoder}

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.duration.DurationInt
import scala.util.Random

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
      PropertyDefaultValue.Double(123.0),
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
      defaultValue = Some(PropertyDefaultValue.Double(1.0)),
      description = Some("Test numeric property"),
      name = Some("numeric-property-prop-1"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Float64)
    )

    val jsonStr = Encoder[ContainerPropertyDefinition].apply(containerProperty).noSpaces
    val decodedContainerProperty = io.circe.parser.parse(jsonStr).flatMap(Decoder[ContainerPropertyDefinition].decodeJson)

    Some(containerProperty) shouldBe decodedContainerProperty.toOption
  }

  it should "CRUD a container" in {
    // TODO: Verify all properties after they fix the bugs
    val containerExternalId = s"vehicle_container_${Random.nextInt(1000)}"
    val containerToCreate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"vehicle-container"),
      description = Some("Test container for modeling vehicles"),
      usedFor = Some(Usage.All),
      properties = VehicleContainerProperties,
      constraints = Some(VehicleContainerConstraints),
      indexes = Some(VehicleContainerIndexes)
    )

    val createdResponse = blueFieldClient.containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()
    createdResponse.isEmpty shouldBe false

    val readAfterCreateContainers = blueFieldClient.containers.retrieveByExternalIds(Seq(ContainerId(space, containerExternalId))).unsafeRunSync()
    val insertedContainer = readAfterCreateContainers.find(_.externalId == containerExternalId)

    insertedContainer.isEmpty shouldBe false
    insertedContainer.get.properties.keys.toList should contain theSameElementsAs VehicleContainerProperties.keys.toList
//    insertedContainer.get.properties.values.toList should contain theSameElementsAs vehicleContainerProperties.values.toList

    val containerToUpdate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"vehicle-container-updated"),
      description = Some("Test container for modeling vehicles updated"),
      usedFor = Some(Usage.All),
      properties = UpdatedVehicleContainerProperties,
      constraints = Some(VehicleContainerConstraints),
      indexes = Some(VehicleContainerIndexes)
    )

    val updatedResponse = blueFieldClient.containers.createItems(containers = Seq(containerToUpdate)).unsafeRunSync()
    updatedResponse.isEmpty shouldBe false

    // TODO: Check update reflection delay and remove 10.seconds sleep
    val readAfterUpdateContainers = (IO.sleep(10.seconds) *> blueFieldClient.containers.retrieveByExternalIds(Seq(ContainerId(space, containerExternalId)))).unsafeRunSync()
    val updatedContainer = readAfterUpdateContainers.find(_.externalId == containerExternalId)

    updatedContainer.isEmpty shouldBe false
    updatedContainer.get.properties.keys.toList should contain theSameElementsAs UpdatedVehicleContainerProperties.keys.toList
    updatedContainer.get.name.get.endsWith("updated") shouldBe true
    updatedContainer.get.description.get.endsWith("updated") shouldBe true

    val deletedContainer = blueFieldClient.containers.delete(Seq(ContainerId(space, containerExternalId))).unsafeRunSync()
    deletedContainer.length shouldBe 1
  }
}

object ContainersTest {

  object VehicleContainer {
    val VehicleContainerProperties: Map[String, ContainerPropertyDefinition] = Map(
      "id" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("unique vehicle id"),
        name = Some("vehicle-identifier"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
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
        description = Some("vehicle engine displacement in CC"),
        name = Some("vehicle-engine-displacement"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32),
        nullable = Some(true)
      ),
      "weight" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle weight in Kg"),
        name = Some("vehicle-weight"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Int64),
        nullable = Some(false)
      ),
      "compressionRatio" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("engine compression ratio"),
        name = Some("compressionRatio"),
        `type` = TextProperty(),
        nullable = Some(true)
      ),
      "turbocharger" -> ContainerPropertyDefinition(
        defaultValue = Some(PropertyDefaultValue.Boolean(false)),
        description = Some("turbocharger availability"),
        name = Some("turbocharger"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
        nullable = Some(false)
      )
    )

    val VehicleContainerConstraints: Map[String, ContainerConstraint] = Map(
      "uniqueId" -> ContainerConstraint.UniquenessConstraint(Seq("id"))
    )

    val VehicleContainerIndexes: Map[String, IndexDefinition] = Map(
      "manufacturerIndex" -> IndexDefinition.BTreeIndexDefinition(Seq("manufacturer")),
      "modelIndex" -> IndexDefinition.BTreeIndexDefinition(Seq("model"))
    )

    val UpdatedVehicleContainerProperties: Map[String, ContainerPropertyDefinition] = VehicleContainerProperties + ("hybrid" -> ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Boolean(false)),
      description = Some("hybrid feature availability for the vehicle"),
      name = Some("hybrid"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
      nullable = Some(true)
    ))

    def vehicleInstanceData(containerRef: ContainerReference): Seq[EdgeOrNodeData] = Seq(
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("1"),
            "manufacturer" -> InstancePropertyValue.String("Toyota"),
            "model" -> InstancePropertyValue.String("RAV-4"),
            "year" -> InstancePropertyValue.Integer(2020),
            "displacement" -> InstancePropertyValue.Integer(2487),
            "weight" -> InstancePropertyValue.Integer(1200L),
            "compressionRatio" -> InstancePropertyValue.String("13 to 1"),
            "turbocharger" -> InstancePropertyValue.Boolean(true)
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("2"),
            "manufacturer" -> InstancePropertyValue.String("Toyota"),
            "model" -> InstancePropertyValue.String("Prius"),
            "year" -> InstancePropertyValue.Integer(2018),
            "displacement" -> InstancePropertyValue.Integer(2487),
            "weight" -> InstancePropertyValue.Integer(1800L),
            "compressionRatio" -> InstancePropertyValue.String("13 to 1"),
            "turbocharger" -> InstancePropertyValue.Boolean(true)
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("3"),
            "manufacturer" -> InstancePropertyValue.String("Volkswagen"),
            "model" -> InstancePropertyValue.String("ID.4"),
            "year" -> InstancePropertyValue.Integer(2022),
            "weight" -> InstancePropertyValue.Integer(2224),
            "turbocharger" -> InstancePropertyValue.Boolean(false)
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("4"),
            "manufacturer" -> InstancePropertyValue.String("Volvo"),
            "model" -> InstancePropertyValue.String("XC90"),
            "year" -> InstancePropertyValue.Integer(2002),
            "weight" -> InstancePropertyValue.Integer(2020),
            "compressionRatio" -> InstancePropertyValue.String("17 to 1"),
            "displacement" -> InstancePropertyValue.Integer(2401),
            "turbocharger" -> InstancePropertyValue.Boolean(true)
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("5"),
            "manufacturer" -> InstancePropertyValue.String("Volvo"),
            "model" -> InstancePropertyValue.String("XC90"),
            "year" -> InstancePropertyValue.Integer(2002),
            "weight" -> InstancePropertyValue.Integer(2020),
            "compressionRatio" -> InstancePropertyValue.String("17 to 1"),
            "displacement" -> InstancePropertyValue.Integer(2401),
            "turbocharger" -> InstancePropertyValue.Boolean(true)
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "id" -> InstancePropertyValue.String("6"),
            "manufacturer" -> InstancePropertyValue.String("Mitsubishi"),
            "model" -> InstancePropertyValue.String("Outlander"),
            "year" -> InstancePropertyValue.Integer(2021),
            "weight" -> InstancePropertyValue.Integer(1745),
            "compressionRatio" -> InstancePropertyValue.String("17 to 1"),
            "displacement" -> InstancePropertyValue.Integer(2000),
            "turbocharger" -> InstancePropertyValue.Boolean(true)
          )
        )
      )
    )
  }

  object RentableContainer {
    val RentableContainerProperties: Map[String, ContainerPropertyDefinition] = Map(
      "item-id" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("rented item id"),
        name = Some("rented-item-id"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "renter-id" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("id of the person renting the item"),
        name = Some("renter-id"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "from" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("item rented from"),
        name = Some("rented-from"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Timestamp),
        nullable = Some(false)
      ),
      "to" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("item rented to"),
        name = Some("rented-to"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Timestamp),
        nullable = Some(false)
      ),
      "invoice-id" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("invoice id for the rent payment"),
        name = Some("invoice-id"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
    )

    val RentableContainerConstraints: Map[String, ContainerConstraint] = Map(
      "unique-id" -> ContainerConstraint.UniquenessConstraint(Seq("item-id", "renter-id", "from"))
    )

    val RentableContainerIndexes: Map[String, IndexDefinition] = Map(
      "renter-index" -> IndexDefinition.BTreeIndexDefinition(Seq("renter-id")),
      "item-index" -> IndexDefinition.BTreeIndexDefinition(Seq("item-id"))
    )

    def rentableInstanceData(containerRef: ContainerReference): Seq[EdgeOrNodeData] = Seq(
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "item-id" -> InstancePropertyValue.String("1"),
            "renter-id" -> InstancePropertyValue.String("222222"),
            "from" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 1, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
            "to" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 1, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
            "invoice-id" -> InstancePropertyValue.String("inv-1"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "item-id" -> InstancePropertyValue.String("1"),
            "renter-id" -> InstancePropertyValue.String("222222"),
            "from" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 2, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
            "to" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 2, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
            "invoice-id" -> InstancePropertyValue.String("inv-2"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "item-id" -> InstancePropertyValue.String("2"),
            "renter-id" -> InstancePropertyValue.String("333333"),
            "from" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 2, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
            "to" -> InstancePropertyValue.Timestamp(ZonedDateTime.of(2020, 2, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
            "invoice-id" -> InstancePropertyValue.String("inv-3"),
          )
        )
      )
    )
  }

  object PersonContainer {
    val PersonContainerProperties: Map[String, ContainerPropertyDefinition] = Map(
      "national-id" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("national identification number"),
        name = Some("national-id"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "firstname" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("firstname of the person"),
        name = Some("firstname"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "lastname" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("lastname of the person"),
        name = Some("lastname"),
        `type` = TextProperty(),
        nullable = Some(false)
      ),
      "dob" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("vehicle model"),
        name = Some("vehicle-model-name"),
        `type` = PrimitiveProperty(`type` = PrimitivePropType.Date),
        nullable = Some(false)
      ),
      "nationality" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("nationality by birth"),
        name = Some("nationality"),
        `type` = TextProperty(),
        nullable = Some(false)
      )
    )

    val PersonContainerConstraints: Map[String, ContainerConstraint] = Map(
      "national-id-nationality" -> ContainerConstraint.UniquenessConstraint(Seq("national-id", "nationality"))
    )

    val PersonContainerIndexes: Map[String, IndexDefinition] = Map(
      "nationality-index" -> IndexDefinition.BTreeIndexDefinition(Seq("nationality")),
      "national-id-index" -> IndexDefinition.BTreeIndexDefinition(Seq("national-id")),
      "firstname-index" -> IndexDefinition.BTreeIndexDefinition(Seq("firstname-id"))
    )

    def personInstanceData(containerRef: ContainerReference): Seq[EdgeOrNodeData] = Seq(
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("111111"),
            "firstname" -> InstancePropertyValue.String("Sadio"),
            "lastname" -> InstancePropertyValue.String("Mane"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1989-11-23")),
            "nationality" -> InstancePropertyValue.String("Senegalese"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("222222"),
            "firstname" -> InstancePropertyValue.String("Alexander"),
            "lastname" -> InstancePropertyValue.String("Arnold"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1989-10-23")),
            "nationality" -> InstancePropertyValue.String("British"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("333333"),
            "firstname" -> InstancePropertyValue.String("Harry"),
            "lastname" -> InstancePropertyValue.String("Kane"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1990-10-20")),
            "nationality" -> InstancePropertyValue.String("British"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("444444"),
            "firstname" -> InstancePropertyValue.String("John"),
            "lastname" -> InstancePropertyValue.String("Gotty"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1978-09-20")),
            "nationality" -> InstancePropertyValue.String("Italian"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("555555"),
            "firstname" -> InstancePropertyValue.String("Angela"),
            "lastname" -> InstancePropertyValue.String("Merkel"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1978-05-20")),
            "nationality" -> InstancePropertyValue.String("German"),
          )
        )
      ),
      EdgeOrNodeData(
        source = containerRef,
        properties = Some(
          Map(
            "national-id" -> InstancePropertyValue.String("666666"),
            "firstname" -> InstancePropertyValue.String("Elon"),
            "lastname" -> InstancePropertyValue.String("Musk"),
            "dob" -> InstancePropertyValue.Object(io.circe.Json.fromString("1982-05-20")),
            "nationality" -> InstancePropertyValue.String("American"),
          )
        )
      )
    )
  }
}
