package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils.{createEdgeWriteData, createNodeWriteData, createTestContainer}
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerCreateDefinition, ContainerId, ContainerReference}
import com.cognite.sdk.scala.v1.fdm.views._

import java.time.temporal.ChronoUnit
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
class InstancesTest extends CommonDataModelTestHelper with RetryWhile {
  private val space = "test-space-scala-sdk"

  ignore should "pass" in {
    blueFieldClient.containers.delete(Seq(
      ContainerId(space, "test_edge_node_container"),
      ContainerId(space, "test_edge_container"),
      ContainerId(space, "test_node_container_1"),
      ContainerId(space, "test_node_container_2")
    )).unsafeRunSync()

    blueFieldClient.views.deleteItems(
      (1 to 23).map(i => s"v${i.toString}").flatMap { v =>
      Seq(
        DataModelReference(space, "test_edge_view", v),
        DataModelReference(space, "test_node_view_1", v),
        DataModelReference(space, "test_node_view_2", v),
        DataModelReference(space, "test_edge_node_view", v)
      )
    }).unsafeRunSync()

    1 shouldBe 1
  }

  it should "CRUD instances with all property types" in {
    val allContainerCreateDefinition = createTestContainer(space, "test_edge_node_container", Usage.All)
    val edgeContainerCreateDefinition = createTestContainer(space, "test_edge_container", Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, "test_node_container_1", Usage.Node)
    val nodeContainerCreateDefinition2 = createTestContainer(space, "test_node_container_2", Usage.Node)

    val viewVersion = "v2"
    val allViewExternalId = s"test_edge_node_view"
    val edgeViewExternalId = s"test_edge_view"
    val nodeViewExternalId1 = s"test_node_view_1"
    val nodeViewExternalId2 = s"test_node_view_2"

    val containersCreated = createContainers(Seq(
      allContainerCreateDefinition,
      edgeContainerCreateDefinition,
      nodeContainerCreateDefinition1,
      nodeContainerCreateDefinition2
    )).unsafeRunSync()
    containersCreated.size shouldBe 4

    val createdViewsMap = (IO.sleep(2.seconds) *> createViews(Seq(
      toViewCreateDef(allViewExternalId, viewVersion, allContainerCreateDefinition),
      toViewCreateDef(edgeViewExternalId, viewVersion, edgeContainerCreateDefinition),
      toViewCreateDef(nodeViewExternalId1, viewVersion, nodeContainerCreateDefinition1),
      toViewCreateDef(nodeViewExternalId2, viewVersion, nodeContainerCreateDefinition2)
    ))).unsafeRunSync()

    val nodeView1 = createdViewsMap(nodeViewExternalId1)
    val nodeView2 = createdViewsMap(nodeViewExternalId2)
    val edgeView = createdViewsMap(edgeViewExternalId)
    val allView = createdViewsMap(allViewExternalId)

    val node1WriteData = createNodeWriteData(
      space,
      s"node_ext_id_$nodeViewExternalId1",
      nodeView1.toSourceReference,
      nodeView1.properties
    )
    val node2WriteData = createNodeWriteData(
      space,
      s"node_ext_id_$nodeViewExternalId2",
      nodeView2.toSourceReference,
      nodeView2.properties
    )
    val startNode = DirectRelationReference(space, externalId = node1WriteData.externalId)
    val endNode = DirectRelationReference(space, externalId = node2WriteData.externalId)
    val edgeWriteData = createEdgeWriteData(
      space,
      s"edge_ext_id_$edgeViewExternalId",
      edgeView.toSourceReference,
      edgeView.properties,
      startNode = startNode,
      endNode = endNode
    )
    val nodeOrEdgeWriteData = Seq(
      createEdgeWriteData(
        space,
        s"nodes_or_edges_ext_id_${allViewExternalId}_edges",
        allView.toSourceReference,
        allView.properties,
        startNode = startNode,
        endNode = endNode
      ),
      createNodeWriteData(
        space,
        s"nodes_or_edges_ext_id_${allViewExternalId}_nodes",
        allView.toSourceReference,
        allView.properties
      )
    )

    (IO.sleep(3.seconds) *> createInstance(Seq(node1WriteData))).unsafeRunSync()
    createInstance(Seq(node2WriteData)).unsafeRunSync()
    createInstance(Seq(edgeWriteData)).unsafeRunSync()
    createInstance(nodeOrEdgeWriteData).unsafeRunSync()

    val readNodesMapOfNode1 = (IO.sleep(3.seconds) *> fetchNodeInstance(nodeView1.toSourceReference, node1WriteData.externalId)).unsafeRunSync()
    val readNodesMapOfNode2 = fetchNodeInstance(nodeView2.toSourceReference, node2WriteData.externalId).unsafeRunSync()
    val readEdgesMapOfEdge = fetchEdgeInstance(edgeView.toSourceReference, edgeWriteData.externalId).unsafeRunSync()
    val readEdgesMapOfAll = fetchEdgeInstance(allView.toSourceReference, nodeOrEdgeWriteData.head.externalId).unsafeRunSync()
    val readNodesMapOfAll = fetchNodeInstance(allView.toSourceReference, nodeOrEdgeWriteData(1).externalId).unsafeRunSync()

    instantPropertyMapEquals(writeDataToMap(node1WriteData), readNodesMapOfNode1) shouldBe true
    instantPropertyMapEquals(writeDataToMap(node2WriteData), readNodesMapOfNode2) shouldBe true
    instantPropertyMapEquals(writeDataToMap(edgeWriteData), readEdgesMapOfEdge) shouldBe true
    instantPropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData.head), readEdgesMapOfAll) shouldBe true
    instantPropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData(1)), readNodesMapOfAll) shouldBe true
  }

  private def writeDataToMap(writeData: NodeOrEdgeCreate) = writeData match {
    case n: NodeOrEdgeCreate.NodeWrite => n.sources.flatMap(d => d.properties.getOrElse(Map.empty)).toMap
    case e: NodeOrEdgeCreate.EdgeWrite => e.sources.flatMap(d => d.properties.getOrElse(Map.empty)).toMap
  }

  private def createContainers(items: Seq[ContainerCreateDefinition]) = {
    blueFieldClient.containers.createItems(items).map(r => r.map(v => v.externalId -> v).toMap)
  }

  private def createViews(items: Seq[ViewCreateDefinition]) = {
    blueFieldClient.views.createItems(items).map(r => r.map(v => v.externalId -> v).toMap)
  }

  private def createInstance(writeData: Seq[NodeOrEdgeCreate]): IO[Seq[SlimNodeOrEdge]] = {
    blueFieldClient.instances.createItems(
      InstanceCreate(items = writeData)
    )
  }

  private def fetchNodeInstance(viewRef: ViewReference, instanceExternalId: String) = {
    blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, instanceExternalId, viewRef.space, Some(Seq(InstanceSource(viewRef))))),
      includeTyping = true
    ).map { r =>
      r.items.collect {
        case n: InstanceDefinition.NodeDefinition => n
      }.map { n =>
        n.properties.getOrElse(Map.empty).values.flatMap(_.values).foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
      }.foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
    }
  }

  private def fetchEdgeInstance(viewRef: ViewReference, instanceExternalId: String) = {
    blueFieldClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Edge, instanceExternalId, viewRef.space, Some(Seq(InstanceSource(viewRef))))),
      includeTyping = true
    ).map { r =>
      r.items.collect {
        case n: InstanceDefinition.EdgeDefinition => n
      }.map { n =>
        n.properties.getOrElse(Map.empty).values.flatMap(_.values).foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
      }.foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
    }
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
      implements = None
    )
  }

  // compare timestamps adhering to the accepted format
  private def instantPropertyMapEquals(expected: Map[String, InstancePropertyValue], actual: Map[String, InstancePropertyValue]): Boolean = {
    val sizeEquals = actual.size === expected.size
    sizeEquals && expected.forall {
      case (k, expectedVal) =>
        val keyEquals = actual.contains(k)

        def valueEquals = {
          val formatter = InstancePropertyValue.Timestamp.formatter
          (actual(k), expectedVal) match {
            case (actVal: InstancePropertyValue.Timestamp, expVal: InstancePropertyValue.Timestamp) =>
              val expTs = expVal.value.truncatedTo(ChronoUnit.SECONDS)
              expTs
                .format(formatter) === actVal
                .value
                .truncatedTo(ChronoUnit.SECONDS)
                .format(formatter)
            case (actVal: InstancePropertyValue.TimestampList, expVal: InstancePropertyValue.TimestampList) =>
              val expTsSeq = expVal
                .value
                .map(_.truncatedTo(ChronoUnit.SECONDS).format(formatter))
              expTsSeq === actVal
                .value
                .map(_.truncatedTo(ChronoUnit.SECONDS).format(formatter))
            case (actVal, expVal) if actVal === expVal => true
            case (actVal, expVal) => fail(s"Actual: ${actVal.toString}, Expected: ${expVal.toString}")
          }
        }

        keyEquals && valueEquals
    }
  }
}
