package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils.{createNodeWriteData, createTestContainer}
import com.cognite.sdk.scala.v1.fdm.common.Usage

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.IterableOps"
  )
)
// scalastyle:off
class InstancesTest extends CommonDataModelTestHelper with RetryWhile {
  private val space = "test-space-scala-sdk"

//  ignore should "CRUD instances" in {
//    val vehicleNodeExternalId = s"vehicles-node-${Random.nextInt(1000).toString}"
//    val vehicleContainerReference = vehicleContainerCreated.toContainerReference
//    val vehicleInstancesToCreate = InstanceCreate(
//      items = Seq(NodeWrite(space, vehicleNodeExternalId, vehicleInstanceData(vehicleContainerReference))),
//      autoCreateStartNodes = Some(true),
//      autoCreateEndNodes = Some(true),
//      replace = Some(true)
//    )
//    val createdVehicleInstances = blueFieldClient.instances.createItems(vehicleInstancesToCreate).unsafeRunSync()
//    createdVehicleInstances.length shouldBe 6
//
//    val personNodeExternalId = s"persons-node-${Random.nextInt(1000).toString}"
//    val personContainerReference = personContainerCreated.toContainerReference
//    val personInstancesToCreate = InstanceCreate(
//      items = Seq(NodeWrite(space, personNodeExternalId, personInstanceData(personContainerReference))),
//      autoCreateStartNodes = Some(true),
//      autoCreateEndNodes = Some(true),
//      replace = Some(true)
//    )
//    val createdPersonInstances = blueFieldClient.instances.createItems(personInstancesToCreate).unsafeRunSync()
//    createdPersonInstances.length shouldBe 6
//
//    val rentableEdgeExternalId = s"rentable-edge-${Random.nextInt(1000).toString}"
//    val rentableContainerReference = rentableContainerCreated.toContainerReference
//    val rentableInstancesToCreate = InstanceCreate(
//      items = Seq(
//        EdgeWrite(
//          `type` = DirectRelationReference(space, rentableEdgeExternalId),
//          space = space,
//          externalId = rentableEdgeExternalId,
//          startNode = DirectRelationReference(space, vehicleNodeExternalId),
//          endNode = DirectRelationReference(space, personNodeExternalId),
//          sources = rentableInstanceData(rentableContainerReference)
//        )
//      ),
//      autoCreateStartNodes = Some(true),
//      autoCreateEndNodes = Some(true),
//      replace = Some(true)
//    )
//    val createdRentableInstances = blueFieldClient.instances.createItems(rentableInstancesToCreate).unsafeRunSync()
//    createdRentableInstances.length shouldBe 3
//
////    val deletedInstances = blueFieldClient.instances.delete(Seq(NodeDeletionRequest(space = space, externalId = vehicleNodeExternalId))).unsafeRunSync()
////    deletedInstances.length shouldBe 1
//  }

  it should "CRUD instances with all possible" in {
//    val containerExternalId = "test_container_896"
    val allContainerCreateDefinition = createTestContainer(space, "test_all_container", Usage.All)
    val edgeContainerCreateDefinition = createTestContainer(space, "test_edge_container", Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, "test_node_container_1", Usage.Node)
    val nodeContainerCreateDefinition2 = createTestContainer(space, "test_node_container_2", Usage.Node)

    val containersCreated = blueFieldClient.containers.createItems(
      Seq(allContainerCreateDefinition, edgeContainerCreateDefinition, nodeContainerCreateDefinition1, nodeContainerCreateDefinition2)
    ).unsafeRunSync()
    containersCreated.length shouldBe 4

    val createdContainersMap = containersCreated.map(c => c.externalId -> c).toMap
//    val allContainer = containersCreated.head
//    val edgeContainer = containersCreated(1)
    val nodeContainer1 = createdContainersMap(nodeContainerCreateDefinition1.externalId)
//    val nodeContainer2 = createdContainersMap(nodeContainerCreateDefinition2.externalId)

    val nodeContainer1CreatedInstances = blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(createNodeWriteData(nodeContainer1)))
    ).unsafeRunSync()

    nodeContainer1CreatedInstances.isEmpty shouldBe false
  }
}
