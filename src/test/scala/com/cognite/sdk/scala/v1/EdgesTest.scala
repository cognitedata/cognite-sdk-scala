// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{CdpApiException, RetryWhile}
import com.cognite.sdk.scala.v1.resources.EdgeQuery
import org.scalatest.BeforeAndAfterAll

import java.time.LocalDate
//import java.util.UUID
import scala.collection.immutable.Seq

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.AnyVal"
  )
)
class EdgesTest extends CommonDataModelTestHelper with RetryWhile with BeforeAndAfterAll {
  // val uuid = UUID.randomUUID.toString
  val fixedUuid = "9e8401a6" // TODO use uuid when we can delete model
  val dataPropString = DataModelPropertyDefinition(PropertyType.Text)
  val dataPropBool = DataModelPropertyDefinition(PropertyType.Boolean)
  val dataPropFloat = DataModelPropertyDefinition(PropertyType.Float32, nullable = false)
  val dataPropDirectRelation = DataModelPropertyDefinition(PropertyType.DirectRelation)
  val dataPropDate = DataModelPropertyDefinition(PropertyType.Date)

  val dataModelNode = DataModel(
    s"Equipment-${fixedUuid}-node",
    Some(
      Map(
        "prop_string" -> dataPropString,
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat,
        "prop_direct_relation" -> dataPropDirectRelation,
        "prop_date" -> dataPropDate
      )
    )
  )

  val dataModelEdge = DataModel(
    s"Equipment-${fixedUuid}-edge",
    Some(
      Map(
        "prop_string" -> dataPropString,
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat,
        "prop_direct_relation" -> dataPropDirectRelation,
        "prop_date" -> dataPropDate
      )
    ),
    dataModelType = DataModelType.EdgeType
  )

  val directRelation = DataModelPropertyDefinition(
    PropertyType.DirectRelation,
    false,
    Some(DataModelIdentifier(None, "node"))
  )
  val nullableDirectRelation = DataModelPropertyDefinition(
    PropertyType.DirectRelation,
    true,
    Some(DataModelIdentifier(None, "node"))
  )

  val newfixedUuid = "fcae0ec0"
  val simpleModelEdge = DataModel(
    s"Equipment-${newfixedUuid}-edge",
    Some(
      Map(
        "startNode" -> dataPropString,
        "type" -> nullableDirectRelation,
        "prop_float" -> dataPropFloat,
        "endNode" -> dataPropDate
      )
    ),
    dataModelType = DataModelType.EdgeType
  )

  val nodeToCreate1 =
    Node(
      "node_1",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Float32.Property(0.1f),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22))
        )
      )
    )

  val nodeToCreate2 =
    Node(
      "node_2",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0002"),
          "prop_bool" -> PropertyType.Boolean.Property(true),
          "prop_float" -> PropertyType.Float32.Property(1.64f)
        )
      )
    )

  val nodeToCreate3 =
    Node(
      "node_3",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0011"),
          "prop_bool" -> PropertyType.Boolean.Property(false),
          "prop_float" -> PropertyType.Float32.Property(3.5f)
        )
      )
    )

  val nodeToCreates = Seq(nodeToCreate1, nodeToCreate2, nodeToCreate3)

  val edgeToCreate1 =
    Edge(
      "edge_12",
      `type` = "edge_12",
      startNode = "node_1",
      endNode = "node_2",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Float32.Property(0.1f),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22))
        )
      )
    )

  val edgeToCreate2 =
    Edge(
      "edge_13",
      `type` = "edge_13",
      startNode = "node_1",
      endNode = "node_3",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0002"),
          "prop_bool" -> PropertyType.Boolean.Property(true),
          "prop_float" -> PropertyType.Float32.Property(1.64f)
        )
      )
    )

  val edgeToCreate3 =
    Edge(
      "edge_23",
      `type` = "edge_23",
      startNode = "node_2",
      endNode = "node_3",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0011"),
          "prop_bool" -> PropertyType.Boolean.Property(false),
          "prop_float" -> PropertyType.Float32.Property(3.5f)
        )
      )
    )

  val edgesToCreates = Seq(edgeToCreate1, edgeToCreate2, edgeToCreate3)

  val simpleEdge1 = Edge(
    "simpleEdge_12",
    `type` = "simpleEdge_12",
    startNode = "node_1",
    endNode = "node_2",
    properties = Some(
      Map("prop_float" -> PropertyType.Float32.Property(0.1f))
    )
  )
  val simpleEdgesToCreates = Seq(simpleEdge1)

  /*val dataPropArrayString = DataModelPropertyDefinition(PropertyType.Array.Text, true)
  val dataPropArrayFloat = DataModelPropertyDefinition(PropertyType.Array.Float32, true)
  val dataPropArrayInt = DataModelPropertyDefinition(PropertyType.Array.Int, true)

  val dataModelArray = DataModel(
    s"Equipment-arry",
    Some(
      Map(
        "array_string" -> dataPropArrayString,
        "array_float" -> dataPropArrayFloat,
        "array_int" -> dataPropArrayInt
      )
    )
  )

  val dmiArrayToCreate1 = Edge(
    "equipment_42",
    properties = Some(
      Map(
        "array_string" -> PropertyType.Array.Text.Property(
            Vector("E101","E102","E103")
          ),
        "array_float" -> PropertyType.Array.Float32.Property(
            Vector(1.01f,1.02f)
          ),
        "array_int" -> PropertyType.Array.Int.Property(
          Vector(1,12,13)
          )
        )
      )
    )

  val dmiArrayToCreate2 = Edge(
    "equipment_43",
    properties = Some(
      Map(
        "array_string" -> PropertyType.Array.Text.Property(
          Vector("E201","E202")
        ),
        "array_float" -> PropertyType.Array.Float32.Property(
          Vector(2.02f, 2.04f)
        )
      )
    )
  )
  val dmiArrayToCreate3 = Edge(
    "equipment_44",
    properties = Some(
      Map(
        "array_float" -> PropertyType.Array.Float32.Property(
          Vector(3.01f,3.02f)
        ),
        "array_int" -> PropertyType.Array.Int.Property(
          Vector(3,12,13)
        )
      )
    )
  )

  val dmiArrayToCreates =
    Seq(dmiArrayToCreate1, dmiArrayToCreate2, dmiArrayToCreate3)*/

  private val space = "test-space"

  override def beforeAll(): Unit = {
    blueFieldClient.dataModels
      .createItems(Seq(simpleModelEdge), space)
      .unsafeRunSync()

    /*blueFieldClient.dataModels
      .createItems(Seq(dataModelNode, dataModelEdge), space)
      .unsafeRunSync() // TODO use this with we can delete model*/

    /*retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => {
        val dmSet = dm.map(m => m.externalId)
        dmSet.contains(dataModelNode.externalId) &&
        dmSet.contains(dataModelEdge.externalId) shouldBe true
      }

    )*/
    ()
  }

  override def afterAll(): Unit =
    /*blueFieldClient.dataModels
      .deleteItems(Seq(dataModel.externalId, dataModelArray.externalId), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe false
    )*/
    ()

  "Insert data model edges" should "work with multiple input" in {
    val nodes = blueFieldClient.nodes
      .createItems(
        space,
        DataModelIdentifier(Some(space), dataModelNode.externalId),
        items = nodeToCreates
      )
      .unsafeRunSync()
      .toList

    nodes.size shouldBe 3
    nodes.map(_.externalId).toSet shouldBe nodeToCreates.map(_.externalId).toSet

    val edges = blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), dataModelEdge.externalId),
        items = simpleEdgesToCreates
      )
      .unsafeRunSync()
      .toList

    edges.size shouldBe 1
    edges.map(_.externalId).toSet shouldBe simpleEdgesToCreates.map(_.externalId).toSet
  }

  it should "fail if node reference does not exist" in {
    val invalidInput = Edge(
      "edge_12",
      `type` = "edge_12",
      startNode = "non_existing_node",
      endNode = "node_2",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Text.Property("abc"),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22))
        )
      )
    )
    val exception = the[CdpApiException] thrownBy blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), dataModelEdge.externalId),
        items = Seq(invalidInput)
      )
      .unsafeRunSync()

    exception.message.contains("Non-existing node referenced") shouldBe true

  }

  it should "fail if input data type is not correct" in {
    val invalidInput = Edge(
      "edge_12",
      `type` = "edge_12",
      startNode = "node_1",
      endNode = "node_2",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Text.Property("abc"),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22))
        )
      )
    )
    val exception = the[CdpApiException] thrownBy blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), dataModelEdge.externalId),
        items = Seq(invalidInput)
      )
      .unsafeRunSync()

    exception.message.contains("invalid input") shouldBe true
    exception.message.contains("abc") shouldBe true
  }

  private def insertEdgesBeforeQuery() = {
    val dataModelInstances = blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), simpleModelEdge.externalId),
        items = simpleEdgesToCreates
      )
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 1
    dataModelInstances
  }

  private def deleteEdgesAfterQuery() = {
    /*val toDeletes = edgesToCreates.map(_.externalId)
    blueFieldClient.edges.deleteByExternalIds(toDeletes).unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId)
    )
    val outputNoFilter = blueFieldClient.edges
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true*/
  }

  private def initAndCleanUpDataForQuery(testCode: Seq[PropertyMap] => Any): Unit =
    try {
      val dataModelInstances = insertEdgesBeforeQuery()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteEdgesAfterQuery()
      ()
    }

  /*private def fromCreatedToExpectedProps(edges: Set[PropertyMap]) =
    edges.map(_.allProperties)*/

  "Query data model instances" should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = EdgeQuery(
      DataModelIdentifier(Some(space), simpleModelEdge.externalId)
    )
    val outputNoFilter = blueFieldClient.edges
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    outputNoFilter.items.toList.size shouldBe simpleEdgesToCreates.length
  }

  /*it should "work with AND filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryAnd = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLAndFilter(
        Seq(
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_string"),
            PropertyType.Text.Property("EQ0002")
          ),
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_bool"),
            PropertyType.Boolean.Property(true)
          ),
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_float"),
            PropertyType.Float32.Property(1.64f)
          )
        )
      )
    )
    val outputQueryAnd = blueFieldClient.edges
      .query(inputQueryAnd)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAnd.size shouldBe 1

    outputQueryAnd.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2)
    )

    val inputQueryAnd2 = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLAndFilter(
        Seq(
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_string"),
            PropertyType.Text.Property("EQ0001")
          ),
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_bool"),
            PropertyType.Boolean.Property(true)
          )
        )
      )
    )
    val outputQueryAndEmpty = blueFieldClient.edges
      .query(inputQueryAnd2)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAndEmpty.isEmpty shouldBe true
  }*/

  /*it should "work with OR filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLOrFilter(
        Seq(
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_string"),
            PropertyType.Text.Property("EQ0011")
          ),
          DSLEqualsFilter(
            Seq(space, dataModelEdge.externalId, "prop_bool"),
            PropertyType.Boolean.Property(true)
          )
        )
      )
    )
    val outputQueryOr = blueFieldClient.edges
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 2
    outputQueryOr.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    )
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLNotFilter(
        DSLInFilter(
          Seq(space, dataModelEdge.externalId, "prop_string"),
          Seq(PropertyType.Text.Property("EQ0002"), PropertyType.Text.Property("EQ0011"))
        )
      )
    )
    val outputQueryNot = blueFieldClient.edges
      .query(inputQueryNot)
      .unsafeRunSync()
      .items
      .toList

    outputQueryNot.size shouldBe 1
    outputQueryNot.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate1)
    )
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLPrefixFilter(
        Seq(space, dataModelEdge.externalId, "prop_string"),
        PropertyType.Text.Property("EQ000")
      )
    )
    val outputQueryPrefix = blueFieldClient.edges
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate1, edgeToCreate2)
    )
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLRangeFilter(
        Seq(space, dataModelEdge.externalId, "prop_float"),
        gte = Some(PropertyType.Float32.Property(1.64f))
      )
    )
    val outputQueryRange = blueFieldClient.edges
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    )
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = EdgeQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLExistsFilter(Seq(space, dataModelEdge.externalId, "prop_bool"))
    )
    val outputQueryExists = blueFieldClient.edges
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    )
  }*/

  "Delete edges" should "work with multiple externalIds" in {
    val toDeletes = edgesToCreates.map(_.externalId)

    blueFieldClient.edges
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = EdgeQuery(DataModelIdentifier(Some(space), dataModelEdge.externalId))
    val outputNoFilter = blueFieldClient.edges
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  it should "ignore unknown externalId" in {
    noException should be thrownBy blueFieldClient.edges
      .deleteByExternalIds(Seq("toto"))
      .unsafeRunSync()
  }

}
