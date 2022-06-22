// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{CdpApiException, DSLExistsFilter, DSLInFilter, DSLNotFilter, DSLOrFilter, DSLPrefixFilter, DSLRangeFilter}
import com.cognite.sdk.scala.common.{DSLAndFilter, DSLEqualsFilter, RetryWhile}
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
    s"Equipment-${fixedUuid}-ed_ge",
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
    s"Equipment-${newfixedUuid}-e",
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
      "ed_ge_12",
      `type` = "ed_ge_12",
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
      "ed_ge_13",
      `type` = "ed_ge13",
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
      "ed_ge_23",
      `type` = "ed_ge23",
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
    "simpleed_ge12",
    `type` = "simpleed_ge12",
    startNode = "node_1",
    endNode = "node_2",
    properties = Some(
      Map("prop_float" -> PropertyType.Float32.Property(0.1f))
    )
  )
  val simpleEdgesToCreates = Seq(simpleEdge1)

  val dataPropArrayString = DataModelPropertyDefinition(PropertyType.Array.Text, true)
  val dataPropArrayFloat = DataModelPropertyDefinition(PropertyType.Array.Float32, true)
  val dataPropArrayInt = DataModelPropertyDefinition(PropertyType.Array.Int, true)


  private val space = "test-space"

  private def filterFloatProperty(m: Map[String, DataModelProperty[_]]) = m.filterKeys{p: String => p !== "prop_float"}

  override def beforeAll(): Unit = {
    blueFieldClient.dataModels
      .createItems(Seq(simpleModelEdge, dataModelNode, dataModelEdge), space)
      .unsafeRunSync()

    blueFieldClient.nodes.createItems(space, DataModelIdentifier(Some(space), dataModelNode.externalId), items = nodeToCreates)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => {
        val dmSet = dm.map(m => m.externalId)
        dmSet.contains(dataModelNode.externalId) &&
        dmSet.contains(dataModelEdge.externalId) shouldBe true
      }

    )
    ()
  }

  override def afterAll(): Unit = {
    blueFieldClient.nodes.deleteByExternalIds(nodeToCreates.map(_.externalId))
      .unsafeRunSync()

    blueFieldClient.edges.deleteByExternalId(simpleModelEdge.externalId)
      .unsafeRunSync()

    blueFieldClient.edges.deleteByExternalIds(edgesToCreates.map(_.externalId))
      .unsafeRunSync()

    /*blueFieldClient.dataModels
      .deleteItems(Seq(dataModel.externalId, dataModelArray.externalId), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe false
    )
     */

    ()
  }


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
          DataModelIdentifier(Some(space), simpleModelEdge.externalId),
          items = simpleEdgesToCreates
        )
        .unsafeRunSync()
        .toList

      edges.size shouldBe 1
      edges.map(_.externalId).toSet shouldBe simpleEdgesToCreates.map(_.externalId).toSet
    }

    it should "fail if node reference does not exist" in {
      val invalidInput = Edge(
        "ed_ge12",
        `type` = "ed_ge12",
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
      exception.message.contains("Unknown resource of type node: '<cannot-be-determined>'") shouldBe true

    }

    it should "fail if input data type is not correct" in {
      val invalidInput = Edge(
        "ed_ge12",
        `type` = "ed_ge12",
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
    val simpleModelInstances = blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), simpleModelEdge.externalId),
        items = simpleEdgesToCreates
      )
      .unsafeRunSync()
      .toList

    val dataModelInstances = blueFieldClient.edges
      .createItems(
        space,
        DataModelIdentifier(Some(space), dataModelEdge.externalId),
        items = edgesToCreates
      )
      .unsafeRunSync()
      .toList

    simpleModelInstances.size shouldBe 1
    dataModelInstances.size shouldBe 3
    dataModelInstances ++ simpleModelInstances
  }

  private def deleteEdgesAfterQuery() = {
    val toDeletes = edgesToCreates.map(_.externalId)
    blueFieldClient.edges.deleteByExternalIds(toDeletes).unsafeRunSync()
    val simpleToDeletes = simpleEdgesToCreates.map(_.externalId)
    blueFieldClient.edges.deleteByExternalIds(simpleToDeletes).unsafeRunSync()

    blueFieldClient.edges.deleteByExternalIds(simpleEdgesToCreates.map(_.externalId)).unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId)
    )
    val outputNoFilter = blueFieldClient.edges
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList

    val simpleEdgeOut = blueFieldClient.edges.retrieveByExternalIds(      DataModelIdentifier(Some(space), simpleModelEdge.externalId)
      , simpleToDeletes).unsafeRunSync().items.toList
    simpleEdgeOut.isEmpty shouldBe true
    outputNoFilter.isEmpty shouldBe true
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

  private def fromCreatedToExpectedProps(edges: Set[PropertyMap]) =
    edges.map(_.allProperties)

  it should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = DataModelInstanceQuery(
      DataModelIdentifier(Some(space), simpleModelEdge.externalId)
    )
    val outputNoFilter = blueFieldClient.edges
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    outputNoFilter.items.toList.size shouldBe simpleEdgesToCreates.length
  }

  it should "work with AND filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryAnd = DataModelInstanceQuery(
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
    val res = outputQueryAnd.map(_.allProperties).toSet.headOption.map(filterFloatProperty)
    val expected = fromCreatedToExpectedProps(Set(edgeToCreate2)).headOption.map(filterFloatProperty)
    res shouldBe expected

    val inputQueryAnd2 = DataModelInstanceQuery(
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
  }

  it should "work with OR filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
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
    val res = outputQueryOr.map(_.allProperties).toSet.map(filterFloatProperty)
    val resExpected = fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    ).map(filterFloatProperty)
   res shouldBe resExpected
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = DataModelInstanceQuery(
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
    outputQueryNot.map(_.allProperties).toSet.map(filterFloatProperty) shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate1)
    ).map(filterFloatProperty)
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
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
    outputQueryPrefix.map(_.allProperties).toSet.map(filterFloatProperty) shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate1, edgeToCreate2)
    ).map(filterFloatProperty)
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = DataModelInstanceQuery(
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

    outputQueryRange.map(_.allProperties).toSet.map(filterFloatProperty) shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    ).map(filterFloatProperty)
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      DataModelIdentifier(Some(space), dataModelEdge.externalId),
      DSLExistsFilter(Seq(space, dataModelEdge.externalId, "prop_bool"))
    )
    val outputQueryExists = blueFieldClient.edges
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties).toSet.map(filterFloatProperty) shouldBe fromCreatedToExpectedProps(
      Set(edgeToCreate2, edgeToCreate3)
    ).map(filterFloatProperty)
  }

  "Delete edges" should "work with multiple externalIds" in {
    val toDeletes = edgesToCreates.map(_.externalId)

    blueFieldClient.edges
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModelEdge.externalId))
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