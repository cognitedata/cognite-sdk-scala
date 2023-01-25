package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils.{createEdgeWriteData, createNodeWriteData, createTestContainer}
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerCreateDefinition, ContainerId, ContainerReference}
import com.cognite.sdk.scala.v1.fdm.views._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration.DurationInt

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
class InstancesTest extends CommonDataModelTestHelper with RetryWhile with BeforeAndAfterAll{
  private val space = "test-space-scala-sdk"

  ignore should "pass" in {
    blueFieldClient.containers.delete(Seq(
      ContainerId(space, "test_edge_node_container"),
      ContainerId(space, "test_edge_container"),
      ContainerId(space, "test_node_container_1"),
      ContainerId(space, "test_node_container_2"),
    )).unsafeRunSync()

    blueFieldClient.views.deleteItems(Seq(
      DataModelReference(space, "test_edge_view", "v1"),
      DataModelReference(space, "test_node_view_1", "v1"),
      DataModelReference(space, "test_node_view_2", "v1"),
      DataModelReference(space, "test_edge_node_view", "v1"),
    )).unsafeRunSync()

    1 shouldBe 1
  }

  it should "CRUD instances with all possible" in {
    val allContainerCreateDefinition = createTestContainer(space, "test_edge_node_container", Usage.All)
    val edgeContainerCreateDefinition = createTestContainer(space, "test_edge_container", Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, "test_node_container_1", Usage.Node)
    val nodeContainerCreateDefinition2 = createTestContainer(space, "test_node_container_2", Usage.Node)

    val viewVersion = "v1"
    val allViewExternalId = s"test_edge_node_view"
    val edgeViewExternalId = s"test_edge_view"
    val nodeViewExternalId1 = s"test_node_view_1"
    val nodeViewExternalId2 = s"test_node_view_2"

    val containersCreated = blueFieldClient.containers.createItems(
      Seq(
        allContainerCreateDefinition,
        edgeContainerCreateDefinition,
        nodeContainerCreateDefinition1,
        nodeContainerCreateDefinition2
      )
    ).unsafeRunSync()
    containersCreated.length shouldBe 4

    val createdViewsMap = (IO.sleep(2.seconds) *> blueFieldClient.views.createItems(
      items = Seq(
        toViewCreateDef(allViewExternalId, viewVersion, allContainerCreateDefinition),
        toViewCreateDef(edgeViewExternalId, viewVersion, edgeContainerCreateDefinition),
        toViewCreateDef(nodeViewExternalId1, viewVersion, nodeContainerCreateDefinition1),
        toViewCreateDef(nodeViewExternalId2, viewVersion, nodeContainerCreateDefinition2),
      )
    ))
      .unsafeRunSync().map(v => v.externalId -> v).toMap

    val nodeView1 = createdViewsMap(nodeViewExternalId1)
    val nodeView2 = createdViewsMap(nodeViewExternalId2)
    val edgeView = createdViewsMap(edgeViewExternalId)
    val allView = createdViewsMap(allViewExternalId)

    val node1WriteData = createNodeWriteData(space, s"node_ext_id_$nodeViewExternalId1", nodeView1.toSourceReference, nodeView1.properties)
    val node2WriteData = createNodeWriteData(space, s"node_ext_id_$nodeViewExternalId2", nodeView2.toSourceReference, nodeView2.properties)

    val startNode = DirectRelationReference(space, externalId = node1WriteData.externalId)
    val endNode = DirectRelationReference(space, externalId = node2WriteData.externalId)
    val edgeWriteData = createEdgeWriteData(space, s"edge_ext_id_$edgeViewExternalId", edgeView.toSourceReference, edgeView.properties, startNode = startNode, endNode = endNode)
    val nodeOrEdgeWriteData = Seq(
      createEdgeWriteData(space, s"nodes_or_edges_ext_id_${allViewExternalId}_edges", allView.toSourceReference, allView.properties, startNode = startNode, endNode = endNode),
      createNodeWriteData(space, s"nodes_or_edges_ext_id_${allViewExternalId}_nodes", allView.toSourceReference, allView.properties)
    )

    val nodeView1Instances = (IO.sleep(2.seconds) *> blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(node1WriteData))
    )).unsafeRunSync()
    val nodeView2Instances = blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(node2WriteData))
    ).unsafeRunSync()
    val nodeEdgeInstances = blueFieldClient.instances.createItems(
      InstanceCreate(items = Seq(edgeWriteData))
    ).unsafeRunSync()
    val nodeOrEdgeInstances = blueFieldClient.instances.createItems(
      InstanceCreate(items = nodeOrEdgeWriteData)
    ).unsafeRunSync()

    nodeView1Instances.isEmpty shouldBe false
    nodeView2Instances.isEmpty shouldBe false
    nodeEdgeInstances.isEmpty shouldBe false
    nodeOrEdgeInstances.isEmpty shouldBe false

    val readNodesOfNode1 = (IO.sleep(2.seconds) *> blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, node1WriteData.externalId, node1WriteData.space, Some(Seq(InstanceSource(nodeView1.toSourceReference)))))
    )).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }
    val readNodesOfNode2 = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, node2WriteData.externalId, node2WriteData.space, Some(Seq(InstanceSource(nodeView2.toSourceReference)))))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }
    val readEdges = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Edge, edgeWriteData.externalId, edgeWriteData.space, Some(Seq(InstanceSource(edgeView.toSourceReference)))))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }
    val readEdgesOfAll = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Edge, nodeOrEdgeWriteData.head.externalId, space, Some(Seq(InstanceSource(allView.toSourceReference)))))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.EdgeDefinition => n
    }
    val readNodesOfAll = blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, nodeOrEdgeWriteData(1).externalId, space, Some(Seq(InstanceSource(allView.toSourceReference)))))
    ).unsafeRunSync().items.collect {
      case n: InstanceDefinition.NodeDefinition => n
    }

    readNodesOfNode1.isEmpty shouldBe false
    readNodesOfNode2.isEmpty shouldBe false
    readEdges.isEmpty shouldBe false
    readEdgesOfAll.isEmpty shouldBe false
    readNodesOfAll.isEmpty shouldBe false

    val node1PropMap = readNodesOfNode1.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap
    val node2PropMap = readNodesOfNode2.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap
    val edgesPropMap = readEdges.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap
    val nodesOfNodesOrEdgesPropMap = readNodesOfAll.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap
    val edgedOfNodesOrEdgesPropMap = readEdgesOfAll.map(n => n.externalId -> n.properties.getOrElse(Map.empty)).toMap

    node1PropMap.nonEmpty shouldBe true
    node2PropMap.nonEmpty shouldBe true
    edgesPropMap.nonEmpty shouldBe true
    nodesOfNodesOrEdgesPropMap.nonEmpty shouldBe true
    edgedOfNodesOrEdgesPropMap.nonEmpty shouldBe true
  }

  private def toViewCreateDef(viewExternalId: String, viewVersion: String, container: ContainerCreateDefinition): ViewCreateDefinition = {
    ViewCreateDefinition(
      space = space,
      externalId = viewExternalId,
      version = Some(viewVersion),
      name = Some(s"Test-View-Sdk-Scala-${container.externalId}"),
      description = Some(s"Test View For Sdk Scala ${container.externalId}"),
      filter = None,
      properties = container.properties.map {
        case (pName, _) => pName -> CreatePropertyReference(ContainerReference(container.space, container.externalId), pName)
      },
      implements = None,
    )
  }

  override def beforeAll(): Unit = {
    //    blueFieldClient.containers.delete(
    //      containersRefs = Seq(
    //        ContainerId(space, "test_edge_node_container"),
    //        ContainerId(space, "test_edge_container"),
    //        ContainerId(space, "test_node_container_1"),
    //        ContainerId(space, "test_node_container_2"),
    //      )
    //    ).unsafeRunSync()
    ()
  }

  override def afterAll(): Unit = {
    //    blueFieldClient.containers.delete(
    //      containersRefs = Seq(
    //        ContainerId(space, "test_edge_node_container"),
    //        ContainerId(space, "test_edge_container"),
    //        ContainerId(space, "test_node_container_1"),
    //        ContainerId(space, "test_node_container_2"),
    //      )
    //    ).unsafeRunSync()
    ()
  }
}
