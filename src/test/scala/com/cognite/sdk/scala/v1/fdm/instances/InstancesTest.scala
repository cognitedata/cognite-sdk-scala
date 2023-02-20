package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.Utils.{createNodeWriteData, createTestContainer}
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{ContainerPropertyDefinition, ViewCorePropertyDefinition}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerCreateDefinition, ContainerReference}
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
class InstancesTest extends CommonDataModelTestHelper {
  private val space = Utils.SpaceExternalId

//  private val edgeNodeContainerExtId = "sdkTest4EdgeNodeContainer"
//  private val edgeContainerExtId = "sdkTest4EdgeContainer"
  private val nodeContainer1ExtId = "sdkTest4NodeContainer1"
//  private val nodeContainer2ExtId = "sdkTest4NodeContainer2"
  private val containerForDirectNodeRelationExtId = Utils.DirectNodeRelationContainerExtId

//  private val edgeNodeViewExtId = "sdkTest4EdgeNodeView"
//  private val edgeViewExtId = "sdkTest4EdgeView"
  private val nodeView1ExtId = "sdkTest4NodeView1"
//  private val nodeView2ExtId = "sdkTest4NodeView2"
  private val viewForDirectNodeRelationExtId = Utils.DirectNodeRelationViewExtId

  private val viewVersion = Utils.ViewVersion

  it should "CRUD instances with all property types" in {

//    blueFieldClient.spacesv3.createItems(Seq(SpaceCreateDefinition(space = Utils.SpaceExternalId))).unsafeRunSync()

//    deleteContainers(Seq(
//      ContainerId(space, edgeNodeContainerExtId),
//      ContainerId(space, edgeContainerExtId),
//      ContainerId(space, nodeContainer1ExtId),
//      ContainerId(space, nodeContainer2ExtId),
//      ContainerId(space, containerForDirectNodeRelationExtId)
//    ))
//
//    deleteViews(Seq(
//      DataModelReference(space, edgeNodeViewExtId, viewVersion),
//      DataModelReference(space, edgeViewExtId, viewVersion),
//      DataModelReference(space, nodeView1ExtId, viewVersion),
//      DataModelReference(space, nodeView2ExtId, viewVersion),
//      DataModelReference(space, viewForDirectNodeRelationExtId, viewVersion)
//    ))

    createContainerForDirectNodeRelations.unsafeRunSync()

//    val allContainerCreateDefinition = createTestContainer(space, edgeNodeContainerExtId, Usage.All)
//    val edgeContainerCreateDefinition = createTestContainer(space, edgeContainerExtId, Usage.Edge)
    val nodeContainerCreateDefinition1 = createTestContainer(space, nodeContainer1ExtId, Usage.Node)
//    val nodeContainerCreateDefinition2 = createTestContainer(space, nodeContainer2ExtId, Usage.Node)

    val containersCreated = createContainers(Seq(
//      allContainerCreateDefinition,
//      edgeContainerCreateDefinition,
      nodeContainerCreateDefinition1,
//      nodeContainerCreateDefinition2
    )).unsafeRunSync()
    containersCreated.size shouldBe 1

//    val allViewExternalId = edgeNodeViewExtId
//    val edgeViewExternalId = edgeViewExtId
    val nodeViewExternalId1 = nodeView1ExtId
//    val nodeViewExternalId2 = nodeView2ExtId

    val createdViewsMap = (IO.sleep(2.seconds) *> createViews(Seq(
//      toViewCreateDef(allViewExternalId, viewVersion, allContainerCreateDefinition),
//      toViewCreateDef(edgeViewExternalId, viewVersion, edgeContainerCreateDefinition),
      toViewCreateDef(nodeViewExternalId1, viewVersion, nodeContainerCreateDefinition1),
//      toViewCreateDef(nodeViewExternalId2, viewVersion, nodeContainerCreateDefinition2)
    ))).unsafeRunSync()

    val nodeView1 = createdViewsMap(nodeViewExternalId1)
//    val nodeView2 = createdViewsMap(nodeViewExternalId2)
//    val edgeView = createdViewsMap(edgeViewExternalId)
//    val allView = createdViewsMap(allViewExternalId)

    val node1WriteData = createNodeWriteData(
      space,
      s"nodeExtId$nodeViewExternalId1",
      nodeView1.toSourceReference,
      nodeView1.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
    )
//    val node2WriteData = createNodeWriteData(
//      space,
//      s"nodeExtId$nodeViewExternalId2",
//      nodeView2.toSourceReference,
//      nodeView2.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
//    )
//    val startNode = DirectRelationReference(space, externalId = node1WriteData.externalId)
//    val endNode = DirectRelationReference(space, externalId = node2WriteData.externalId)
//    val edgeWriteData = createEdgeWriteData(
//      space,
//      s"edgeExtId$edgeViewExternalId",
//      edgeView.toSourceReference,
//      edgeView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p},
//      startNode = startNode,
//      endNode = endNode
//    )
//    val nodeOrEdgeWriteData = Seq(
//      createEdgeWriteData(
//        space,
//        s"nodesOrEdgesExtId${allViewExternalId}Edges",
//        allView.toSourceReference,
//        allView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p},
//        startNode = startNode,
//        endNode = endNode
//      ),
//      createNodeWriteData(
//        space,
//        s"nodesOrEdgesExtId${allViewExternalId}Nodes",
//        allView.toSourceReference,
//        allView.properties.collect { case (n, p: ViewCorePropertyDefinition) => n -> p}
//      )
//    )

    (IO.sleep(2.seconds) *> createInstance(Seq(node1WriteData))).unsafeRunSync()
//    createInstance(Seq(node2WriteData)).unsafeRunSync()
//    createInstance(Seq(edgeWriteData)).unsafeRunSync()
//    createInstance(nodeOrEdgeWriteData).unsafeRunSync()

    val readNodesMapOfNode1 = (IO.sleep(3.seconds) *> fetchNodeInstance(nodeView1.toSourceReference, node1WriteData.externalId)).unsafeRunSync()
//    val readNodesMapOfNode2 = fetchNodeInstance(nodeView2.toSourceReference, node2WriteData.externalId).unsafeRunSync()
//    val readEdgesMapOfEdge = fetchEdgeInstance(edgeView.toSourceReference, edgeWriteData.externalId).unsafeRunSync()
//    val readEdgesMapOfAll = fetchEdgeInstance(allView.toSourceReference, nodeOrEdgeWriteData.head.externalId).unsafeRunSync()
//    val readNodesMapOfAll = fetchNodeInstance(allView.toSourceReference, nodeOrEdgeWriteData(1).externalId).unsafeRunSync()

    instantPropertyMapEquals(writeDataToMap(node1WriteData), readNodesMapOfNode1) shouldBe true
//    instantPropertyMapEquals(writeDataToMap(node2WriteData), readNodesMapOfNode2) shouldBe true
//    instantPropertyMapEquals(writeDataToMap(edgeWriteData), readEdgesMapOfEdge) shouldBe true
//    instantPropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData.head), readEdgesMapOfAll) shouldBe true
//    instantPropertyMapEquals(writeDataToMap(nodeOrEdgeWriteData(1)), readNodesMapOfAll) shouldBe true

//    val deletedInstances = deleteInstance(
//      Seq(
//        NodeDeletionRequest(node1WriteData.space, node1WriteData.externalId),
//        NodeDeletionRequest(node2WriteData.space, node2WriteData.externalId),
//        EdgeDeletionRequest(edgeWriteData.space, edgeWriteData.externalId)
//      )
//        ++ nodeOrEdgeWriteData.map {
//        case n: NodeOrEdgeCreate.NodeWrite => NodeDeletionRequest(n.space, n.externalId)
//        case e: NodeOrEdgeCreate.EdgeWrite => EdgeDeletionRequest(e.space, e.externalId)
//      }
//    )
//    deletedInstances.length shouldBe 1
//
//    val deletedViews = deleteViews(Seq(
//      DataModelReference(space, edgeViewExternalId, viewVersion),
//      DataModelReference(space, nodeViewExternalId1, viewVersion),
//      DataModelReference(space, nodeViewExternalId2, viewVersion),
//      DataModelReference(space, allViewExternalId, viewVersion)
//    ))
//
//    deletedViews.length shouldBe 1

//    val deletedContainers = deleteContainers(
//      Seq(
//        ContainerId(space, allContainerCreateDefinition.externalId),
//        ContainerId(space, edgeContainerCreateDefinition.externalId),
//        ContainerId(space, nodeContainerCreateDefinition1.externalId),
//        ContainerId(space, nodeContainerCreateDefinition2.externalId)
//      )
//    )
//    deletedContainers.length shouldBe 1

    1 shouldBe 1
  }

  private def writeDataToMap(writeData: NodeOrEdgeCreate) = writeData match {
    case n: NodeOrEdgeCreate.NodeWrite => n.sources.flatMap(d => d.properties.getOrElse(Map.empty)).toMap
    case e: NodeOrEdgeCreate.EdgeWrite => e.sources.flatMap(d => d.properties.getOrElse(Map.empty)).toMap
  }

  private def createContainers(items: Seq[ContainerCreateDefinition]) = {
    blueFieldClient.containers.createItems(items).flatTap(_ => IO.sleep(2.seconds)).map(r => r.map(v => v.externalId -> v).toMap)
  }

//  private def deleteContainers(items: Seq[ContainerId]) = {
//    blueFieldClient.containers.delete(items).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
//  }

  private def createViews(items: Seq[ViewCreateDefinition]) = {
    blueFieldClient.views.createItems(items).flatTap(_ => IO.sleep(2.seconds)).map(r => r.map(v => v.externalId -> v).toMap)
  }

  private def createInstance(writeData: Seq[NodeOrEdgeCreate]): IO[Seq[SlimNodeOrEdge]] = {
    blueFieldClient.instances.createItems(
      InstanceCreate(items = writeData)
    ).flatTap(_ => IO.sleep(2.seconds))
  }

//  private def deleteInstance(refs: Seq[InstanceDeletionRequest]): Seq[InstanceDeletionRequest] = {
//    blueFieldClient.instances.delete(instanceRefs = refs).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
//  }
//
//  private def deleteViews(items: Seq[DataModelReference]) = {
//    blueFieldClient.views.deleteItems(items).flatTap(_ => IO.sleep(2.seconds)).unsafeRunSync()
//  }

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

//  private def fetchEdgeInstance(viewRef: ViewReference, instanceExternalId: String) = {
//    blueFieldClient.instances.retrieveByExternalIds(items = Seq(
//      InstanceRetrieve(InstanceType.Edge, instanceExternalId, viewRef.space, Some(Seq(InstanceSource(viewRef))))),
//      includeTyping = true
//    ).map { r =>
//      r.items.collect {
//        case n: InstanceDefinition.EdgeDefinition => n
//      }.map { n =>
//        n.properties.getOrElse(Map.empty).values.flatMap(_.values).foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
//      }.foldLeft(Map.empty[String, InstancePropertyValue])((a, b) => a ++ b)
//    }
//  }

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
  private def instantPropertyMapEquals(expected: Map[String, InstancePropertyValue], actual: Map[String, InstancePropertyValue]): Boolean = {
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
        `type` = PropertyType.TextProperty(Some(false), None),
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
