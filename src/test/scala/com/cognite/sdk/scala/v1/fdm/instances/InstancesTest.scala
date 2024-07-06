package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.CdpApiException
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.Utils.{createEdgeWriteData, createNodeWriteData, createTestContainer}
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition.HasData
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{ContainerPropertyDefinition, ViewCorePropertyDefinition}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType
import com.cognite.sdk.scala.v1.fdm.common.{DataModelReference, DirectRelationReference, Usage}
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerCreateDefinition, ContainerId, ContainerReference}
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.{EdgeDeletionRequest, NodeDeletionRequest}
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
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.OptionPartial"
  )
)
class InstancesTest extends CommonDataModelTestHelper {
  private val space = Utils.SpaceExternalId

  private val edgeNodeContainerExtId = "sdkTest13EdgeNodeContainer2"
  private val edgeContainerExtId = "sdkTest13EdgeContainer2"
  private val nodeContainer1ExtId = "sdkTest13NodeContainer3"
  private val nodeContainer2ExtId = "sdkTest13NodeContainer4"
  private val containerForDirectNodeRelationExtId = Utils.DirectNodeRelationContainerExtId

  private val edgeNodeViewExtId = "sdkTest13EdgeNodeView2"
  private val edgeViewExtId = "sdkTest13EdgeView3"
  private val nodeView1ExtId = "sdkTest13NodeView4"
  private val nodeView2ExtId = "sdkTest13NodeView5"
  private val viewForDirectNodeRelationExtId = Utils.DirectNodeRelationViewExtId

  private val viewVersion = Utils.ViewVersion

  it should "CRUD instances with all property types" in {

//    deleteContainers(Seq(
//      ContainerId(space, edgeNodeContainerExtId),
//      ContainerId(space, edgeContainerExtId),
//      ContainerId(space, nodeContainer1ExtId),
//      ContainerId(space, nodeContainer2ExtId),
//      ContainerId(space, containerForDirectNodeRelationExtId)
//    ))
//
//    deleteViews(Seq(
//      DataModelReference(space, edgeNodeViewExtId, Some(viewVersion)),
//      DataModelReference(space, edgeViewExtId, Some(viewVersion)),
//      DataModelReference(space, nodeView1ExtId, Some(viewVersion)),
//      DataModelReference(space, nodeView2ExtId, Some(viewVersion)),
//      DataModelReference(space, viewForDirectNodeRelationExtId, Some(viewVersion))
//    ))

    createContainerForDirectNodeRelations.unsafeRunSync()

    val allContainerCreateDefinition = createTestContainer(space, edgeNodeContainerExtId, Usage.All)
    val edgeContainerCreateDefinition = createTestContainer(space, edgeContainerExtId, Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, nodeContainer1ExtId, Usage.Node)
    val nodeContainerCreateDefinition2 = createTestContainer(space, nodeContainer2ExtId, Usage.Node)

    val containersCreated = createContainers(Seq(
      allContainerCreateDefinition,
      edgeContainerCreateDefinition,
      nodeContainerCreateDefinition1,
      nodeContainerCreateDefinition2
    )).unsafeRunSync()
    containersCreated.size shouldBe 4

    val allViewExternalId = edgeNodeViewExtId
    val edgeViewExternalId = edgeViewExtId
    val nodeViewExternalId1 = nodeView1ExtId
    val nodeViewExternalId2 = nodeView2ExtId

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
      s"nodeExtId$nodeViewExternalId1",
      nodeView1.toSourceReference,
      nodeView1.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
    )
    val node2WriteData = createNodeWriteData(
      space,
      s"nodeExtId$nodeViewExternalId2",
      nodeView2.toSourceReference,
      nodeView2.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
    )
    val startNode = DirectRelationReference(space, externalId = node1WriteData.externalId)
    val endNode = DirectRelationReference(space, externalId = node2WriteData.externalId)
    val edgeWriteData = createEdgeWriteData(
      space,
      s"edgeExtId$edgeViewExternalId",
      edgeView.toSourceReference,
      edgeView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p},
      startNode = startNode,
      endNode = endNode
    )
    val nodeOrEdgeWriteData = Seq(
      createEdgeWriteData(
        space,
        s"nodesOrEdgesExtId${allViewExternalId}Edges",
        allView.toSourceReference,
        allView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p},
        startNode = startNode,
        endNode = endNode
      ),
      createNodeWriteData(
        space,
        s"nodesOrEdgesExtId${allViewExternalId}Nodes",
        allView.toSourceReference,
        allView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
      )
    )

    (IO.sleep(2.seconds) *> createInstance(Seq(node1WriteData))).unsafeRunSync()
    createInstance(Seq(node2WriteData)).unsafeRunSync()
    createInstance(Seq(edgeWriteData)).unsafeRunSync()
    createInstance(nodeOrEdgeWriteData).unsafeRunSync()

    val readNodesMapOfNode1 = (IO.sleep(10.seconds) *> fetchNodeInstance(nodeView1.toSourceReference, node1WriteData.externalId)).unsafeRunSync()
    val readNodesMapOfNode2 = fetchNodeInstance(nodeView2.toSourceReference, node2WriteData.externalId).unsafeRunSync()
    val syncNodesMapOfNodeView1: InstanceSyncResponse = syncNodeInstances(nodeView1.toSourceReference)
      .unsafeRunSync()
    val syncNodesMapOfNodeView2 = syncNodeInstances(nodeView2.toSourceReference).unsafeRunSync()

    syncNodesMapOfNodeView1.nextCursor match { case map =>
      map.size shouldBe 1
      map.keys.exists("sync" === _) shouldBe true
    }

    syncNodesMapOfNodeView2.nextCursor match { case map =>
      map.size shouldBe 1
      map.keys.exists("sync" === _) shouldBe true
    }

    val queryNodesMapOfNodeView1: InstanceQueryResponse = queryNodeInstances(nodeView1.toSourceReference)
      .unsafeRunSync()
    val queryNodesMapOfNodeView2: InstanceQueryResponse = queryNodeInstances(nodeView2.toSourceReference)
      .unsafeRunSync()

    queryNodesMapOfNodeView1.nextCursor.map { map =>
      map.size shouldBe 1
      map.keys.exists("query" === _) shouldBe true
    }

    queryNodesMapOfNodeView2.nextCursor.map { map =>
      map.size shouldBe 1
      map.keys.exists("query" === _) shouldBe true
    }

    val readEdgesMapOfEdge = fetchEdgeInstance(edgeView.toSourceReference, edgeWriteData.externalId).unsafeRunSync()
    val readEdgesMapOfAll = fetchEdgeInstance(allView.toSourceReference, nodeOrEdgeWriteData.head.externalId).unsafeRunSync()
    val readNodesMapOfAll = fetchNodeInstance(allView.toSourceReference, nodeOrEdgeWriteData(1).externalId).unsafeRunSync()

    instancePropertyMapEquals(writeDataToMap(node1WriteData), readNodesMapOfNode1) shouldBe true
    instancePropertyMapEquals(writeDataToMap(node2WriteData), readNodesMapOfNode2) shouldBe true
    instancePropertyMapEquals(writeDataToMap(edgeWriteData), readEdgesMapOfEdge) shouldBe true
    instancePropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData.head), readEdgesMapOfAll) shouldBe true
    instancePropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData(1)), readNodesMapOfAll) shouldBe true

    // Test deletion of properties: Make a new edit from node1WriteData, but with 1 property set to None
    val nodeSource = node1WriteData.sources.get.head
    val nodeProps = nodeSource.properties.get
    val nodeKey = nodeProps.keys.find(k => !k.endsWith("NonNullable")).get
    val nodeEditData = node1WriteData.copy(sources = Some(Seq(nodeSource.copy(properties = Some(nodeProps + (nodeKey -> None))))))
    // Update to delete this property
    createInstance(Seq(nodeEditData)).unsafeRunSync()
    val readEditedNode = fetchNodeInstance(nodeView1.toSourceReference, nodeEditData.externalId).unsafeRunSync()
    val expectedEditedNodeProperties = writeDataToMap(nodeEditData) - nodeKey
    instancePropertyMapEquals(expectedEditedNodeProperties, readEditedNode) shouldBe true

    // Non-nullable properties should fail deletion
    val edgeSource = edgeWriteData.sources.get.head
    val edgeProps = edgeSource.properties.get
    val edgeKey = edgeProps.keys.find(k => k.endsWith("NonNullable")).get
    val edgeEditData = edgeWriteData.copy(sources = Some(Seq(edgeSource.copy(properties = Some(edgeProps + (edgeKey -> None))))))
    val ex = intercept[CdpApiException] { createInstance(Seq(edgeEditData)).unsafeRunSync() }
    ex.code shouldBe 400

    // Unmentioned properties should be left alone
    val emptyEditData = edgeWriteData.copy(sources = Some(Seq(edgeSource.copy(properties = Some(Map.empty)))))
    createInstance(Seq(emptyEditData))
    val readEditedEdge = fetchEdgeInstance(edgeView.toSourceReference, emptyEditData.externalId).unsafeRunSync()
    // The read data equals the original creation data, since the above should be a noop
    instancePropertyMapEquals(writeDataToMap(edgeWriteData), readEditedEdge) shouldBe true

    val deletedInstances = deleteInstance(
      Seq(
        NodeDeletionRequest(node1WriteData.space, node1WriteData.externalId),
        NodeDeletionRequest(node2WriteData.space, node2WriteData.externalId),
        EdgeDeletionRequest(edgeWriteData.space, edgeWriteData.externalId)
      )
        ++ nodeOrEdgeWriteData.map {
        case n: NodeOrEdgeCreate.NodeWrite => NodeDeletionRequest(n.space, n.externalId)
        case e: NodeOrEdgeCreate.EdgeWrite => EdgeDeletionRequest(e.space, e.externalId)
      }
    )
    deletedInstances.length shouldBe 5

    val deletedViews = deleteViews(Seq(
      DataModelReference(space, edgeViewExternalId, Some(viewVersion)),
      DataModelReference(space, nodeViewExternalId1, Some(viewVersion)),
      DataModelReference(space, nodeViewExternalId2, Some(viewVersion)),
      DataModelReference(space, allViewExternalId, Some(viewVersion))
    ))

    deletedViews.length shouldBe 4

    val deletedContainers = deleteContainers(
      Seq(
        ContainerId(space, allContainerCreateDefinition.externalId),
        ContainerId(space, edgeContainerCreateDefinition.externalId),
        ContainerId(space, nodeContainerCreateDefinition1.externalId),
        ContainerId(space, nodeContainerCreateDefinition2.externalId)
      )
    )
    deletedContainers.length shouldBe 4
  }

  private def writeDataToMap(writeData: NodeOrEdgeCreate): Map[String, InstancePropertyValue] = (writeData match {
    case n: NodeOrEdgeCreate.NodeWrite => n.sources
    case e: NodeOrEdgeCreate.EdgeWrite => e.sources
  }).getOrElse(Seq.empty).flatMap(d => d.properties.getOrElse(Map.empty)).flatMap {case (k, v) => v.map(k -> _)}.toMap

  private def createContainers(items: Seq[ContainerCreateDefinition]) = {
    testClient.containers.createItems(items).flatTap(_ => IO.sleep(2.seconds)).map(r => r.map(v => v.externalId -> v).toMap)
  }

  private def deleteContainers(items: Seq[ContainerId]) = {
    testClient.containers.delete(items).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
  }

  private def createViews(items: Seq[ViewCreateDefinition]) = {
    testClient.views.createItems(items).flatTap(_ => IO.sleep(2.seconds)).map(r => r.map(v => v.externalId -> v).toMap)
  }

  private def createInstance(writeData: Seq[NodeOrEdgeCreate]): IO[Seq[SlimNodeOrEdge]] = {
    testClient.instances.createItems(
      InstanceCreate(items = writeData)
    ).flatTap(_ => IO.sleep(2.seconds))
  }

  private def deleteInstance(refs: Seq[InstanceDeletionRequest]): Seq[InstanceDeletionRequest] = {
    testClient.instances.delete(instanceRefs = refs).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
  }

  private def deleteViews(items: Seq[DataModelReference]) = {
    testClient.views.deleteItems(items).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
  }

  private def fetchNodeInstance(viewRef: ViewReference, instanceExternalId: String) = {
    testClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Node, instanceExternalId, viewRef.space)),
      includeTyping = true,
      sources = Some(Seq(InstanceSource(viewRef)))
    ).map { r =>
      r.items.collect {
        case n: InstanceDefinition.NodeDefinition => n
      }.map { n =>
        n.properties.getOrElse(Map.empty).values.flatMap(_.values).foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
      }.foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
    }
  }

  private def syncNodeInstances(viewRef: ViewReference) = {
    val hasData = HasData(Seq(viewRef))

    testClient.instances.syncRequest(
      InstanceSyncRequest(
        `with` = Map("sync" -> TableExpression(nodes = Option(NodesTableExpression(filter = Option(hasData))))),
        cursors = None,
        select = Map("sync" -> SelectExpression(sources =
          List(SourceSelector(source = viewRef, properties = List("*")))))
      )
    )
  }

  private def queryNodeInstances(viewRef: ViewReference) = {
    val hasData = HasData(Seq(viewRef))

    testClient.instances.queryRequest(
      InstanceQueryRequest(
        `with` = Map("query" -> TableExpression(nodes = Option(NodesTableExpression(filter = Option(hasData))))),
        cursors = None,
        select = Map("query" -> SelectExpression(sources =
          List(SourceSelector(source = viewRef, properties = List("*")))))
      )
    )
  }

  private def fetchEdgeInstance(viewRef: ViewReference, instanceExternalId: String) = {
    testClient.instances.retrieveByExternalIds(items = Seq(
      InstanceRetrieve(InstanceType.Edge, instanceExternalId, viewRef.space)),
      includeTyping = true,
      sources = Some(Seq(InstanceSource(viewRef)))
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
      version = viewVersion,
      name = Some(s"Test-View-Sdk-Scala-${container.externalId}"),
      description = Some(s"Test View For Sdk Scala ${container.externalId}"),
      filter = None,
      properties = container.properties.map {
        case (pName, _) => pName -> ViewPropertyCreateDefinition.CreateViewProperty(
          name = Some(pName),
          description = Some(pName),
          container = ContainerReference(container.space, container.externalId),
          containerPropertyIdentifier = pName
        )
      },
      implements = None
    )
  }

  // compare timestamps adhering to the accepted format
  private def instancePropertyMapEquals(expected: Map[String, InstancePropertyValue], actual: Map[String, InstancePropertyValue]): Boolean = {
    val sizeEquals = actual.size === expected.size
    sizeEquals && expected.forall {
      case (k, expectedVal) =>
        val keyEquals = actual.contains(k)

        def valueEquals = {
          val formatter = InstancePropertyValue.Timestamp.formatter
          (actual(k), expectedVal) match {
            case (actVal: InstancePropertyValue.Timestamp, expVal: InstancePropertyValue.Timestamp) =>
              val expTs = expVal.value.truncatedTo(ChronoUnit.MILLIS)
              expTs
                .format(formatter) === actVal
                .value
                .truncatedTo(ChronoUnit.MILLIS)
                .format(formatter)
            case (actVal: InstancePropertyValue.TimestampList, expVal: InstancePropertyValue.TimestampList) =>
              val expTsSeq = expVal
                .value
                .map(_.truncatedTo(ChronoUnit.MILLIS).format(formatter))
              expTsSeq === actVal
                .value
                .map(_.truncatedTo(ChronoUnit.MILLIS).format(formatter))
            case (actVal, expVal) if actVal === expVal => true
            case (actVal, expVal) => fail(s"Actual: ${actVal.toString}, Expected: ${expVal.toString}")
          }
        }

        keyEquals && valueEquals
    }
  }

  private def createContainerForDirectNodeRelations = {
    val nodeContainerProps: Map[String, ContainerPropertyDefinition] = Map(
      "stringProp1" -> ContainerPropertyDefinition(
        nullable = Some(true),
        autoIncrement = None,
        defaultValue = None,
        description = None,
        name = None,
        `type` = PropertyType.TextProperty(Some(false), None)
      )
    )

    val containerCreation = ContainerCreateDefinition(
      space = space,
      externalId = containerForDirectNodeRelationExtId,
      name = Some(s"NodeContainerForDirectRelation"),
      description = Some(s"NodeContainerForDirectRelation"),
      usedFor = Some(Usage.Node),
      properties = nodeContainerProps,
      constraints = None,
      indexes = None
    )

    val viewCreation = toViewCreateDef(viewForDirectNodeRelationExtId, viewVersion, containerCreation)

    (for {
      _ <- createContainers(Seq(containerCreation))
      viewsMap <- createViews(Seq(viewCreation))
    } yield  {
      val instanceData = createNodeWriteData(
        space,
        s"${containerForDirectNodeRelationExtId}Instance",
        ViewReference(space = space, externalId = viewForDirectNodeRelationExtId, version = viewVersion),
        viewsMap(viewForDirectNodeRelationExtId).properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p })
      createInstance(Seq(instanceData))
    }) *> IO.sleep(2.seconds)
  }

}
