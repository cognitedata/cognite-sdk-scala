package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition.HasData
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType
import com.cognite.sdk.scala.v1.fdm.containers.ContainerCreateDefinition
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDefinition.NodeDefinition
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.{ViewCreateDefinition, ViewDefinition, ViewPropertyCreateDefinition, ViewReference}


@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.Var"
  )
)
class InstancesSyncIntegrationTest extends CommonDataModelTestHelper {
  private val nodePropMap = Map(
    "stringProp1" -> ContainerPropertyDefinition(
      nullable = Some(true),
      autoIncrement = Some(false),
      defaultValue = None,
      description = Some("Test TextProperty NonList WithoutDefaultValue Nullable Description"),
      name = Some("Test-TextProperty-NonList-WithoutDefaultValue-Nullable-Name"),
      `type` = PropertyType.TextProperty(Some(false), Some("ucs_basic"))
    )
  )

  private val nodeContainer = {
    testClient.containers
      .createItems(containers =
        Seq(
          ContainerCreateDefinition(
            space = Utils.SpaceExternalId,
            externalId = "testSyncNodeContainer",
            name = Some(s"Test-Container-Scala-Sdk"),
            description = Some(s"Test Container For Scala SDK"),
            usedFor = Some(Usage.All),
            properties = nodePropMap,
            constraints = None,
            indexes = None
          )
        )
      ).unsafeRunSync().headOption
  }

  private val nodeView: Option[ViewDefinition] = {
    testClient.views
      .createItems(items =
        Seq(
          ViewCreateDefinition(
            space = Utils.SpaceExternalId,
            externalId = "testSyncNodeView",
            version = "v1",
            name = Some(s"Test-View-Scala-SDK"),
            description = Some("Test View For Scala SDK"),
            filter = None,
            properties = nodeContainer.map { c =>
              c.properties.map {
                case (pName, _) =>
                  pName -> ViewPropertyCreateDefinition.CreateViewProperty(
                    name = Some(pName),
                    container = c.toSourceReference,
                    containerPropertyIdentifier = pName)
              }
            }.getOrElse(Map.empty),
            implements = None
          )
        )
      ).unsafeRunSync().headOption
  }

  private val viewReference = nodeView.map(v => ViewReference(
    externalId = v.externalId, space = v.space, version = v.version)
  ).getOrElse(throw new Exception("View not found"))

  private def syncNodeInstances(viewRef: ViewReference, cursors: Option[Map[String, String]]) = {
    val hasData = HasData(Seq(viewRef))
    testClient.instances.syncRequest(
      InstanceSyncRequest(
        `with` = Map("sync" -> TableExpression(nodes = Option(NodesTableExpression(filter = Option(hasData))))),
        cursors = cursors,
        select = Map("sync" -> SelectExpression(sources =
          List(SourceSelector(source = viewRef, properties = List("*"))))),
        includeTyping = Some(true)
      )
    )
  }

  private def ingestNodes(stringValues: Seq[String]) = {
    val instances = stringValues.map { stringVal =>
      NodeWrite(
        space = Utils.SpaceExternalId,
        externalId = stringVal,
        sources = Some(Seq(EdgeOrNodeData(source = viewReference, properties = Some(Map("stringProp1" -> Some(InstancePropertyValue.String(stringVal))))))))
    }

    testClient.instances.createItems(InstanceCreate(items = instances)).unsafeRunSync()
  }

  private def deleteNodes(stringValues: Seq[String]) = {
    val nodesToDelete = stringValues.map { stringVal =>
      NodeDeletionRequest(
        space = Utils.SpaceExternalId,
        externalId = stringVal
      )
    }

    testClient.instances.delete(nodesToDelete).unsafeRunSync()
  }

  private def syncAndValidate(viewRef: ViewReference, expected: Seq[String], cursor: Option[Map[String, String]], deleted: Boolean = false) = {
    val syncResponse = syncNodeInstances(viewRef, cursor).unsafeRunSync()
    val nodes: Seq[NodeDefinition] = syncResponse.items.get("sync").asInstanceOf[Seq[NodeDefinition]]
    nodes.size shouldBe expected.size
    nodes.map(_.externalId) should contain theSameElementsAs expected
    nodes.map(_.deletedTime.isDefined) should be (Seq.fill(expected.size)(deleted))
    syncResponse.nextCursor
  }

  @scala.annotation.tailrec
  private def syncToHead(viewRef: ViewReference, cursor: Option[Map[String, String]] = None): Option[Map[String, String]] = {
    val syncResponse = syncNodeInstances(viewRef, cursor).unsafeRunSync()
    val nodes: Seq[NodeDefinition] = syncResponse.items.get("sync").asInstanceOf[Seq[NodeDefinition]]
    if (nodes.nonEmpty) {
      syncToHead(viewRef, syncResponse.nextCursor)
    } else {
      syncResponse.nextCursor
    }
  }

  it should "test sync roundtrip" in {
    // Clean out any existing nodes, and sync cursor to head.
    // Existing nodes may be left over from previous failed tests, and test would fail again since
    // new inserts would be no-op and not synced out. deletion of nodes is also no-op if they don't exist, so test should pass on empty setup.
    val stringValues = Seq("test1", "test2", "test3")
    val stringValues2 = Seq("test4", "test5", "test6")
    deleteNodes(stringValues)
    deleteNodes(stringValues2)
    var cursor = syncToHead(viewReference)

    // Add nodes and validate we can sync new items out:
    ingestNodes(stringValues)
    cursor = syncAndValidate(viewReference, stringValues, cursor)

    // Next cursor has no more data
    cursor = syncAndValidate(viewReference, Seq.empty, cursor)

    // Add more data
    ingestNodes(stringValues2)
    cursor = syncAndValidate(viewReference, stringValues2, cursor)

    // Next cursor has no more data
    cursor = syncAndValidate(viewReference, Seq.empty, cursor)

    // delete and sync again
    deleteNodes(stringValues)
    cursor = syncAndValidate(viewReference, stringValues, cursor, deleted = true)
    cursor = syncAndValidate(viewReference, Seq.empty, cursor)

    // delete and sync again
    deleteNodes(stringValues2)
    cursor = syncAndValidate(viewReference, stringValues2, cursor, deleted = true)
    cursor = syncAndValidate(viewReference, Seq.empty, cursor)
  }
}
