// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.ContainersTest.VehicleContainer._
import com.cognite.sdk.scala.v1.containers.ContainerPropertyType._
import com.cognite.sdk.scala.v1.containers._
import com.cognite.sdk.scala.v1.instances.{InstanceContainerData, InstancePropertyType}
import com.cognite.sdk.scala.v1.resources.Containers._
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

  it should "CRUD a container" in {
    // TODO: Verify all properties after they fix the bugs
    val containerExternalId = s"vehicle_container_${Random.nextInt(1000)}"
    val containerToCreate = ContainerCreate(
      space = space,
      externalId = containerExternalId,
      name = Some(s"vehicle-container"),
      description = Some("Test container for modeling vehicles"),
      usedFor = Some(ContainerUsage.All),
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

    val containerToUpdate = ContainerCreate(
      space = space,
      externalId = containerExternalId,
      name = Some(s"vehicle-container-updated"),
      description = Some("Test container for modeling vehicles updated"),
      usedFor = Some(ContainerUsage.All),
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
      "compression-ratio" -> ContainerPropertyDefinition(
        defaultValue = None,
        description = Some("engine compression ratio"),
        name = Some("compression-ratio"),
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
      "unique-id" -> ContainerConstraint.UniquenessConstraint(Seq("id"))
    )

    val VehicleContainerIndexes: Map[String, IndexDefinition] = Map(
      "manufacturer-index" -> IndexDefinition.BTreeIndexDefinition(Seq("manufacturer")),
      "model-index" -> IndexDefinition.BTreeIndexDefinition(Seq("model"))
    )

    val UpdatedVehicleContainerProperties: Map[String, ContainerPropertyDefinition] = VehicleContainerProperties + ("hybrid" -> ContainerPropertyDefinition(
      defaultValue = Some(PropertyDefaultValue.Boolean(false)),
      description = Some("hybrid feature availability for the vehicle"),
      name = Some("hybrid"),
      `type` = PrimitiveProperty(`type` = PrimitivePropType.Boolean),
      nullable = Some(true)
    ))

    def vehicleInstanceData(containerRef: ContainerReference): Seq[InstanceContainerData] = Seq(
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("1"),
          "manufacturer" -> InstancePropertyType.String("Toyota"),
          "model" -> InstancePropertyType.String("RAV-4"),
          "year" -> InstancePropertyType.Integer(2020),
          "displacement" -> InstancePropertyType.Integer(2487),
          "weight" -> InstancePropertyType.Integer(1200L),
          "compression-ratio" -> InstancePropertyType.String("13:1"),
          "turbocharger" -> InstancePropertyType.Boolean(true)
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("2"),
          "manufacturer" -> InstancePropertyType.String("Toyota"),
          "model" -> InstancePropertyType.String("Prius"),
          "year" -> InstancePropertyType.Integer(2018),
          "displacement" -> InstancePropertyType.Integer(2487),
          "weight" -> InstancePropertyType.Integer(1800L),
          "compression-ratio" -> InstancePropertyType.String("13:1"),
          "turbocharger" -> InstancePropertyType.Boolean(true)
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("3"),
          "manufacturer" -> InstancePropertyType.String("Volkswagen"),
          "model" -> InstancePropertyType.String("ID.4"),
          "year" -> InstancePropertyType.Integer(2022),
          "weight" -> InstancePropertyType.Integer(2224),
          "turbocharger" -> InstancePropertyType.Boolean(false)
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("4"),
          "manufacturer" -> InstancePropertyType.String("Volvo"),
          "model" -> InstancePropertyType.String("XC90"),
          "year" -> InstancePropertyType.Integer(2002),
          "weight" -> InstancePropertyType.Integer(2020),
          "compression-ratio" -> InstancePropertyType.String("17:1"),
          "displacement" -> InstancePropertyType.Integer(2401),
          "turbocharger" -> InstancePropertyType.Boolean(true)
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("5"),
          "manufacturer" -> InstancePropertyType.String("Volvo"),
          "model" -> InstancePropertyType.String("XC90"),
          "year" -> InstancePropertyType.Integer(2002),
          "weight" -> InstancePropertyType.Integer(2020),
          "compression-ratio" -> InstancePropertyType.String("17:1"),
          "displacement" -> InstancePropertyType.Integer(2401),
          "turbocharger" -> InstancePropertyType.Boolean(true)
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "id" -> InstancePropertyType.String("6"),
          "manufacturer" -> InstancePropertyType.String("Mitsubishi"),
          "model" -> InstancePropertyType.String("Outlander"),
          "year" -> InstancePropertyType.Integer(2021),
          "weight" -> InstancePropertyType.Integer(1745),
          "compression-ratio" -> InstancePropertyType.String("17:1"),
          "displacement" -> InstancePropertyType.Integer(2000),
          "turbocharger" -> InstancePropertyType.Boolean(true)
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

    def rentableInstanceData(containerRef: ContainerReference): Seq[InstanceContainerData] = Seq(
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "item-id" -> InstancePropertyType.String("1"),
          "renter-id" -> InstancePropertyType.String("222222"),
          "from" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 1, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
          "to" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 1, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
          "invoice-id" -> InstancePropertyType.String("inv-1"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "item-id" -> InstancePropertyType.String("1"),
          "renter-id" -> InstancePropertyType.String("222222"),
          "from" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 2, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
          "to" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 2, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
          "invoice-id" -> InstancePropertyType.String("inv-2"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "item-id" -> InstancePropertyType.String("2"),
          "renter-id" -> InstancePropertyType.String("333333"),
          "from" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 2, 1, 9, 0, 0, 0, ZoneId.of("GMT+1"))),
          "to" -> InstancePropertyType.Timestamp(ZonedDateTime.of(2020, 2, 14, 18, 0, 0, 0, ZoneId.of("GMT+1"))),
          "invoice-id" -> InstancePropertyType.String("inv-3"),
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

    def personInstanceData(containerRef: ContainerReference): Seq[InstanceContainerData] = Seq(
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("111111"),
          "firstname" -> InstancePropertyType.String("Sadio"),
          "lastname" -> InstancePropertyType.String("Mane"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1989-11-23")),
          "nationality" -> InstancePropertyType.String("Senegalese"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("222222"),
          "firstname" -> InstancePropertyType.String("Alexander"),
          "lastname" -> InstancePropertyType.String("Arnold"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1989-10-23")),
          "nationality" -> InstancePropertyType.String("British"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("333333"),
          "firstname" -> InstancePropertyType.String("Harry"),
          "lastname" -> InstancePropertyType.String("Kane"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1990-10-20")),
          "nationality" -> InstancePropertyType.String("British"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("444444"),
          "firstname" -> InstancePropertyType.String("John"),
          "lastname" -> InstancePropertyType.String("Gotty"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1978-09-20")),
          "nationality" -> InstancePropertyType.String("Italian"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("555555"),
          "firstname" -> InstancePropertyType.String("Angela"),
          "lastname" -> InstancePropertyType.String("Merkel"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1978-05-20")),
          "nationality" -> InstancePropertyType.String("German"),
        )
      ),
      InstanceContainerData(
        container = containerRef,
        properties = Map(
          "national-id" -> InstancePropertyType.String("666666"),
          "firstname" -> InstancePropertyType.String("Elon"),
          "lastname" -> InstancePropertyType.String("Musk"),
          "dob" -> InstancePropertyType.Object(io.circe.Json.fromString("1982-05-20")),
          "nationality" -> InstancePropertyType.String("American"),
        )
      )
    )
  }
}
