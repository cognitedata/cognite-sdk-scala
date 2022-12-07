package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.containers.ContainersTest.PersonContainer._
import com.cognite.sdk.scala.v1.fdm.containers.ContainersTest.RentableContainer._
import com.cognite.sdk.scala.v1.fdm.containers.ContainersTest.VehicleContainer._
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerCreate, ContainerUsage}
import com.cognite.sdk.scala.v1.fdm.instances.InstanceTypeWriteItem._

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
  private val vehicleContainerExternalId = s"vehicle_container_${Random.nextInt(1000)}"
  private val personContainerExternalId = s"person_container_${Random.nextInt(1000)}"
  private val rentableContainerExternalId = s"rentable_container_${Random.nextInt(1000)}"
  private val vehicleContainer = ContainerCreate(
    space = space,
    externalId = vehicleContainerExternalId,
    name = Some(s"vehicle-container"),
    description = Some("vehicle info container"),
    usedFor = Some(ContainerUsage.All),
    properties = VehicleContainerProperties,
    constraints = Some(VehicleContainerConstraints),
    indexes = Some(VehicleContainerIndexes)
  )
  private val personContainer = ContainerCreate(
    space = space,
    externalId = personContainerExternalId,
    name = Some(s"person-container"),
    description = Some("person records container"),
    usedFor = Some(ContainerUsage.All),
    properties = PersonContainerProperties,
    constraints = Some(PersonContainerConstraints),
    indexes = Some(PersonContainerIndexes)
  )
  private val rentableContainer = ContainerCreate(
    space = space,
    externalId = rentableContainerExternalId,
    name = Some(s"rentable-item-container"),
    description = Some("container to make anything rentable"),
    usedFor = Some(ContainerUsage.All),
    properties = RentableContainerProperties,
    constraints = Some(RentableContainerConstraints),
    indexes = Some(RentableContainerIndexes)
  )
  private val vehicleContainerCreated = blueFieldClient.containers.createItems(Seq(vehicleContainer)).unsafeRunSync().head
  private val personContainerCreated = blueFieldClient.containers.createItems(Seq(personContainer)).unsafeRunSync().head
  private val rentableContainerCreated = blueFieldClient.containers.createItems(Seq(rentableContainer)).unsafeRunSync().head

  it should "CRUD instances" in {
    val vehicleNodeExternalId = s"vehicles-node-${Random.nextInt(1000)}"
    val vehicleContainerReference = vehicleContainerCreated.toContainerReference
    val vehicleInstancesToCreate = InstanceCreate(
      items = Seq(NodeContainerWriteItem(space, vehicleNodeExternalId, vehicleInstanceData(vehicleContainerReference))),
      autoCreateStartNodes = Some(true),
      autoCreateEndNodes = Some(true),
      replace = Some(true)
    )
    val createdVehicleInstances = blueFieldClient.instances.createItems(vehicleInstancesToCreate).unsafeRunSync()
    createdVehicleInstances.length shouldBe 6

    val personNodeExternalId = s"persons-node-${Random.nextInt(1000)}"
    val personContainerReference = personContainerCreated.toContainerReference
    val personInstancesToCreate = InstanceCreate(
      items = Seq(NodeContainerWriteItem(space, personNodeExternalId, personInstanceData(personContainerReference))),
      autoCreateStartNodes = Some(true),
      autoCreateEndNodes = Some(true),
      replace = Some(true)
    )
    val createdPersonInstances = blueFieldClient.instances.createItems(personInstancesToCreate).unsafeRunSync()
    createdPersonInstances.length shouldBe 6

    val rentableEdgeExternalId = s"rentable-edge-${Random.nextInt(1000)}"
    val rentableContainerReference = rentableContainerCreated.toContainerReference
    val rentableInstancesToCreate = InstanceCreate(
      items = Seq(
        EdgeContainerWriteItem(
          `type` = DirectRelationReference(space, rentableEdgeExternalId),
          space = space,
          externalId = rentableEdgeExternalId,
          startNode = DirectRelationReference(space, vehicleNodeExternalId),
          endNode = DirectRelationReference(space, personNodeExternalId),
          containers = rentableInstanceData(rentableContainerReference)
        )
      ),
      autoCreateStartNodes = Some(true),
      autoCreateEndNodes = Some(true),
      replace = Some(true)
    )
    val createdRentableInstances = blueFieldClient.instances.createItems(rentableInstancesToCreate).unsafeRunSync()
    createdRentableInstances.length shouldBe 3

//    val deletedInstances = blueFieldClient.instances.delete(Seq(NodeDeletionRequest(space = space, externalId = vehicleNodeExternalId))).unsafeRunSync()
//    deletedInstances.length shouldBe 1
  }
}
