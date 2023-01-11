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

  it should "CRUD instances with all possible" in {
    val allContainerCreateDefinition = createTestContainer(space, "test_edge_node_container", Usage.All)
    val edgeContainerCreateDefinition = createTestContainer(space, "test_edge_container", Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, "test_node_container_1", Usage.Node)
    val nodeContainerCreateDefinition2 = createTestContainer(space, "test_node_container_2", Usage.Node)

    val containersCreated = blueFieldClient.containers.createItems(
      Seq(
        allContainerCreateDefinition,
        edgeContainerCreateDefinition,
        nodeContainerCreateDefinition1,
        nodeContainerCreateDefinition2
      )
    ).unsafeRunSync()
    containersCreated.length shouldBe 4

    val createdContainersMap = containersCreated.map(c => c.externalId -> c).toMap

    val nodeContainer1 = createdContainersMap(nodeContainerCreateDefinition1.externalId)
    val nodeContainer2 = createdContainersMap(nodeContainerCreateDefinition2.externalId)

    val node1WriteData = createNodeWriteData(nodeContainer1)
    val node2WriteData = createNodeWriteData(nodeContainer2)

    val nodeContainer1CreatedInstances = blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(node1WriteData))
    ).unsafeRunSync()
    val nodeContainer2CreatedInstances = blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(node2WriteData))
    ).unsafeRunSync()

    nodeContainer1CreatedInstances.isEmpty shouldBe false
    nodeContainer2CreatedInstances.isEmpty shouldBe false

    val readNodesOfNode1 = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, node1WriteData.externalId, node1WriteData.space, None))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }
    val readNodesOfNode2 = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, node2WriteData.externalId, node2WriteData.space, None))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }

    readNodesOfNode1.isEmpty shouldBe false
    readNodesOfNode2.isEmpty shouldBe false

    val node1PropMap = readNodesOfNode1.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap
    val node2PropMap = readNodesOfNode2.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap

    node1PropMap.nonEmpty shouldBe true
    node2PropMap.nonEmpty shouldBe true
  }
}
