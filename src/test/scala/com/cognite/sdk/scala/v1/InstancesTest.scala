package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.ContainersTest._
import com.cognite.sdk.scala.v1.containers.{ContainerConstraint, ContainerCreate, ContainerRead, ContainerReference, ContainerUsage, IndexDefinition}
import com.cognite.sdk.scala.v1.instances.{InstanceContainerData, InstanceCreate, InstancePropertyType, InstanceTypeWriteItem}

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
class InstancesTest extends CommonDataModelTestHelper with RetryWhile {
  private val space = "test-space-scala-sdk"
  private val containerExternalId = s"vehicle_container_${Random.nextInt(1000)}"
  private val container = ContainerCreate(
    space = space,
    externalId = containerExternalId,
    name = Some(s"vehicle-container-updated"),
    description = Some("Test container for modeling vehicles updated"),
    usedFor = Some(ContainerUsage.All),
    properties = UpdatedVehicleContainerProperties,
    constraints = Some(Map(
      "unique-properties" -> ContainerConstraint.UniquenessConstraint(Seq("manufacturer", "model")))
    ),
    indexes = Some(Map(
      "manufacturer-index" -> IndexDefinition.BTreeIndexDefinition(Seq("manufacturer")),
      "model-index" -> IndexDefinition.BTreeIndexDefinition(Seq("model")))
    )
  )
  private val createdContainer: ContainerRead = blueFieldClient.containers.createItems(Seq(container)).unsafeRunSync().head

  it should "CRUD instances" in {
    val instancesToCreate = InstanceCreate(
      items = Seq(InstanceTypeWriteItem.NodeContainerWriteItem(space, "node-1", Seq(InstanceContainerData(
        container = ContainerReference(space, createdContainer.externalId),
        properties = Map(
          "manufacturer" -> InstancePropertyType.String("Toyota"),
          "model" -> InstancePropertyType.String("RAV-4"),
          "year" -> InstancePropertyType.Integer(2020),
          "displacement" -> InstancePropertyType.Integer(2487),
          "weight" -> InstancePropertyType.Integer(1200L),
          "cylinders" -> InstancePropertyType.Integer(4),
          "compression-ratio" -> InstancePropertyType.String("13:1"),
          "turbocharger" -> InstancePropertyType.Boolean(true),
          "vtec" -> InstancePropertyType.Boolean(false),
        )
      )))),
      autoCreateStartNodes = Some(true),
      autoCreateEndNodes = Some(true),
      replace = Some(true)
    )
    val createdInstances = blueFieldClient.instances.createItems(instancesToCreate).unsafeRunSync()
    createdInstances.length shouldBe 1
  }
}
