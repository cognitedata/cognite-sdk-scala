// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{
  CdpApiException,
  DSLAndFilter,
  DSLContainsAnyFilter,
  DSLEqualsFilter,
  DSLExistsFilter,
  DSLInFilter,
  DSLNotFilter,
  DSLOrFilter,
  DSLPrefixFilter,
  DSLRangeFilter,
  RetryWhile
}
import org.scalatest.{Assertion, BeforeAndAfterAll}

import java.time.LocalDate
import java.util.UUID
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
class NodesTest
    extends CommonDataModelTestHelper
    with RetryWhile
    with BeforeAndAfterAll {
  val uuid = UUID.randomUUID.toString
  val dataPropString = DataModelPropertyDefinition(PropertyType.Text)
  val dataPropBool = DataModelPropertyDefinition(PropertyType.Boolean)
  val dataPropFloat = DataModelPropertyDefinition(PropertyType.Float32, nullable = false)
  val dataPropDirectRelation = DataModelPropertyDefinition(PropertyType.DirectRelation)
  val dataPropDate = DataModelPropertyDefinition(PropertyType.Date)
  val dataPropJson = DataModelPropertyDefinition(PropertyType.Json)
  val dataPropInt = DataModelPropertyDefinition(PropertyType.Int)

  val dataModel = DataModel(
    s"Equipment-instances-0",
    Some(
      Map(
        "prop_string" -> dataPropString,
        "prop_bool" -> dataPropBool,
        "prop_float" -> dataPropFloat,
        "prop_direct_relation" -> dataPropDirectRelation,
        "prop_date" -> dataPropDate,
        "prop_json" -> dataPropJson
      )
    )
  )

  private val space = "test-space"

  val dataModelNodeToCreate1 =
    Node(
      "equipment_43",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Float32.Property(0.1f),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property(List(space, "externalId")),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22)),
          "prop_json" -> PropertyType.Json.Property("""{
                                                      |  "int_val" : 2,
                                                      |  "string_val" : "tata",
                                                      |  "struct_val" : {
                                                      |    "age" : 25.0,
                                                      |    "name" : "jetfire"
                                                      |  }
                                                      |}""".stripMargin)
        )
      )
    )

  val dataModelNodeCreate2 =
    Node(
      "equipment_44",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0002"),
          "prop_bool" -> PropertyType.Boolean.Property(true),
          "prop_float" -> PropertyType.Float32.Property(1.64f)
        )
      )
    )

  val dataModelNodeToCreate3 =
    Node(
      "45",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0011"),
          "prop_bool" -> PropertyType.Boolean.Property(false),
          "prop_float" -> PropertyType.Float32.Property(3.5f)
        )
      )
    )

  val toCreates =
    Seq(dataModelNodeToCreate1, dataModelNodeCreate2, dataModelNodeToCreate3)

  val dataPropArrayString = DataModelPropertyDefinition(PropertyType.Array.Text, true)
  val dataPropArrayFloat = DataModelPropertyDefinition(PropertyType.Array.Float32, true)
  val dataPropArrayInt = DataModelPropertyDefinition(PropertyType.Array.Int, true)
  val dataPropArrayJson = DataModelPropertyDefinition(PropertyType.Array.Json, true)

  val dataModelArray = DataModel(
    s"Equipment-arry",
    Some(
      Map(
        "array_string" -> dataPropArrayString,
        "array_float" -> dataPropArrayFloat,
        "array_int" -> dataPropArrayInt,
        "array_json" -> dataPropArrayJson
      )
    )
  )

  val dmiArrayToCreate1 = Node(
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
          ),
        "array_json" -> PropertyType.Array.Json.Property(
          Vector("""{
                   |  "string_val" : "tata"
                   |}""".stripMargin,
                """{
                  |  "int_val" : 2
                  |}""".stripMargin)
          )
        )
      )
    )

  val dmiArrayToCreate2 = Node(
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
  val dmiArrayToCreate3 = Node(
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
    Seq(dmiArrayToCreate1, dmiArrayToCreate2, dmiArrayToCreate3)

  override def beforeAll(): Unit = {
    blueFieldClient.dataModels
      .createItems(Seq(dataModel, dataModelArray), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => {
        val dmSet = dm.map(m => m.externalId)
        dmSet.contains(dataModel.externalId) &&
          dmSet.contains(dataModelArray.externalId) shouldBe true
      }
    )
    ()
  }

  override def afterAll(): Unit = {
    /*blueFieldClient.dataModels
      .deleteItems(Seq(dataModel.externalId, dataModelArray.externalId), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe false
    )*/
    ()
  }

  "Insert data model instances" should "work with multiple input" in {
    val dataModelInstances = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = toCreates)
      .unsafeRunSync()
      .toList

    dataModelInstances.size shouldBe 3
    dataModelInstances.map(_.externalId).toSet shouldBe toCreates.map(_.externalId).toSet
    dataModelInstances
      .find(_.externalId === dataModelNodeToCreate1.externalId)
      .flatMap(_.allProperties.get("prop_json")) shouldBe Some(
      PropertyType.Json.Property("""{
                                   |  "int_val" : 2,
                                   |  "string_val" : "tata",
                                   |  "struct_val" : {
                                   |    "age" : 25.0,
                                   |    "name" : "jetfire"
                                   |  }
                                   |}""".stripMargin)
    )

    val instancesOfArray = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModelArray.externalId), items = Seq(dmiArrayToCreate1))
      .unsafeRunSync()
      .toList

    instancesOfArray.size shouldBe 1
    instancesOfArray.map(_.externalId).toSet shouldBe Set(dmiArrayToCreate1.externalId)
    instancesOfArray
      .find(_.externalId === dmiArrayToCreate1.externalId)
      .flatMap(_.allProperties.get("array_json")) shouldBe Some(
      PropertyType.Array.Json.Property(Seq("""{
                                             |  "string_val" : "tata"
                                             |}""".stripMargin,
                                             """{
                                             |  "int_val" : 2
                                             |}""".stripMargin))
    )
  }

  it should "fail if input data type is not correct" in {
    val invalidInput = Node(
      "equipment_47",
      properties = Some(
        Map(
          "prop_float" -> PropertyType.Text.Property("abc")
        )
      )
    )
    val exception = the[CdpApiException] thrownBy blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = Seq(invalidInput))
      .unsafeRunSync()
    exception.message shouldBe "Value type mismatch"
  }

  private def insertDMIBeforeQuery() = {
    val dataModelInstances = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = toCreates)
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIAfterQuery() = {
    val toDeletes = toCreates.map(_.externalId)
    blueFieldClient.nodes.deleteItems(toDeletes, space).unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModel.externalId), space)
    val outputNoFilter = blueFieldClient.nodes
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  private def initAndCleanUpDataForQuery(testCode: Seq[PropertyMap] => Any): Unit =
    try {
      val dataModelInstances = insertDMIBeforeQuery()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDMIAfterQuery()
      ()
    }

  private val expectedSpaceExternalIdInProps = Map("spaceExternalId" -> PropertyType.Text.Property(space))

  private def fromCreatedToExpectedProps(instances: Set[PropertyMap]) =
    instances.map(_.allProperties ++ expectedSpaceExternalIdInProps)

  "Query data model instances" should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space
      )
    val outputNoFilter = blueFieldClient.nodes
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    outputNoFilter.items.toList.size shouldBe toCreates.length
  }

  it should "work with AND filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryAnd = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLAndFilter(
        Seq(
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0002")),
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true)),
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_float"), PropertyType.Float32.Property(1.64f))
        )
      )
    )
    val outputQueryAnd = blueFieldClient.nodes
      .query(inputQueryAnd)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAnd.size shouldBe 1

    outputQueryAnd.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeCreate2)
    )

    val inputQueryAnd2 = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLAndFilter(
        Seq(
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0001")),
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
        )
      )
    )
    val outputQueryAndEmpty = blueFieldClient.nodes
      .query(inputQueryAnd2)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAndEmpty.isEmpty shouldBe true
  }

  it should "work with OR filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLOrFilter(
        Seq(
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0011")),
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
        )
      )
    )
    val outputQueryOr = blueFieldClient.nodes
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 2
    outputQueryOr.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeCreate2, dataModelNodeToCreate3)
    )
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLNotFilter(
        DSLInFilter(
          Seq(space, dataModel.externalId, "prop_string"),
          Seq(PropertyType.Text.Property("EQ0002"), PropertyType.Text.Property("EQ0011"))
        )
      )
    )
    val outputQueryNot = blueFieldClient.nodes
      .query(inputQueryNot)
      .unsafeRunSync()
      .items
      .toList

    outputQueryNot.size shouldBe 1
    outputQueryNot.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeToCreate1)
    )
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLPrefixFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ000"))
    )
    val outputQueryPrefix = blueFieldClient.nodes
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeToCreate1, dataModelNodeCreate2)
    )
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLRangeFilter(
        Seq(space, dataModel.externalId, "prop_float"),
        gte = Some(PropertyType.Float32.Property(1.64f))
      )
    )
    val outputQueryRange = blueFieldClient.nodes
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeCreate2, dataModelNodeToCreate3)
    )
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLExistsFilter(Seq(space, dataModel.externalId, "prop_bool"))
    )
    val outputQueryExists = blueFieldClient.nodes
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelNodeCreate2, dataModelNodeToCreate3)
    )
  }

  private def insertDMIArrayBeforeQuery() = {
    val dataModelInstances = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModelArray.externalId), items = dmiArrayToCreates)
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIArrayAfterQuery() = {
    val toDeletes = dmiArrayToCreates.map(_.externalId)
    blueFieldClient.nodes.deleteItems(toDeletes, space).unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModelArray.externalId), space)
    val outputNoFilter = blueFieldClient.nodes
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  private def initAndCleanUpArrayDataForQuery(testCode: Seq[PropertyMap] => Any): Unit =
    try {
      val dataModelInstances = insertDMIArrayBeforeQuery()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDMIArrayAfterQuery()
      ()
    }

  it should "work with CONTAINS ANY filter" in initAndCleanUpArrayDataForQuery { _ =>
    val inputQueryContainsAnyString = DataModelInstanceQuery(
      DataModelIdentifier(Some(space), dataModelArray.externalId),
      space,
      DSLContainsAnyFilter(
        Seq(space, dataModelArray.externalId, "array_string"),
        Seq(
          PropertyType.Text.Property("E201"),
          PropertyType.Text.Property("E103")
        )
      )
    )
    val outputQueryContainsAnyString = blueFieldClient.nodes
      .query(inputQueryContainsAnyString)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAnyString.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dmiArrayToCreate1, dmiArrayToCreate2)
    )

  /*val inputQueryContainsAnyInt = DataModelInstanceQuery(
      dataModelArray.externalId,
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_int"),
          Seq(Int32Property(13))
        )
      )
    )
    val outputQueryContainsAnyInt = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAnyInt)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAnyInt.map(_.properties).toSet shouldBe Set(
      dmiArrayToCreate1,
      dmiArrayToCreate3
    ).map(_.properties)*/

  // VH TODO test float[]
  }

  it should "work with CONTAINS ALL filter" in initAndCleanUpArrayDataForQuery { _ =>
    val inputQueryContainsAllString = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModelArray.externalId),
      space,
      DSLContainsAnyFilter(
        Seq(space, dataModelArray.externalId, "array_string"),
        Seq(
          PropertyType.Text.Property("E201"),
          PropertyType.Text.Property("E202")
        )
      )
    )
    val outputQueryContainsAllString = blueFieldClient.nodes
      .query(inputQueryContainsAllString)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAllString.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dmiArrayToCreate2)
    )

  /*val inputQueryContainsAllInt = DataModelInstanceQuery(
      dataModelArray.externalId,
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_int"),
          Seq(
            Int32Property(12),
            Int32Property(13)
          )
        )
      )
    )
    val outputQueryContainsAllInt = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAllInt)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAllInt.map(_.properties).toSet shouldBe Set(
      dmiArrayToCreate1,
      dmiArrayToCreate3
    ).map(_.properties)*/
  }

  // Not yet supported
  ignore should "work with sort" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLExistsFilter(Seq(dataModel.externalId, "prop_float")),
      Some(Seq(dataModel.externalId, "col_float:desc"))
    )
    val outputQueryExists = blueFieldClient.nodes
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties) shouldBe Seq(
      dataModelNodeToCreate3,
      dataModelNodeCreate2,
      dataModelNodeToCreate1
    ).map(_.properties)
  }

  it should "work with limit" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLOrFilter(
        Seq(
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0011")),
          DSLEqualsFilter(Seq(space, dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
        )
      ),
      None,
      Some(1)
    )
    val outputQueryOr = blueFieldClient.nodes
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 1
    val expected: Set[Map[String, DataModelProperty[_]]] =
      fromCreatedToExpectedProps(Set(dataModelNodeCreate2, dataModelNodeToCreate3))

    outputQueryOr
      .map(_.allProperties)
      .toSet
      .subsetOf(expected) shouldBe true
  }

  it should "work with cursor and stream" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      space,
      DSLPrefixFilter(Seq(space, dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ00"))
    )

    def checkOutputProp(output: Seq[PropertyMap]): Assertion = {
      val expected = fromCreatedToExpectedProps(toCreates.toSet)
      output
        .map(_.allProperties)
        .toSet
        .subsetOf(
          expected
        ) shouldBe true
    }

    val outputLimit1 = blueFieldClient.nodes
      .queryStream(inputQueryPrefix, Some(1))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit1.size shouldBe 1
    checkOutputProp(outputLimit1)

    val outputLimit2 = blueFieldClient.nodes
      .queryStream(inputQueryPrefix, Some(2))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit2.size shouldBe 2
    checkOutputProp(outputLimit2)

    val outputLimit3 = blueFieldClient.nodes
      .queryStream(inputQueryPrefix, Some(3))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit3.size shouldBe 3
    checkOutputProp(outputLimit3)
  }

  "List data model instances" should "work with multiple externalIds" in initAndCleanUpDataForQuery{ _=>
    val outputList = blueFieldClient.nodes
      .retrieveByExternalIds(DataModelIdentifier(Some(space), dataModel.externalId), space, toCreates.map(_.externalId))
      .unsafeRunSync()
      .items
      .toList
    outputList.size shouldBe 3
    outputList.map(_.allProperties).toSet shouldBe toCreates.map(_.allProperties ++ expectedSpaceExternalIdInProps).toSet
  }

  // Not yet supported
  ignore should "raise an exception if input has invalid externalId" in {
    the[CdpApiException] thrownBy blueFieldClient.nodes
      .retrieveByExternalIds(DataModelIdentifier(Some(space), dataModel.externalId), space, Seq("toto"))
      .unsafeRunSync()
      .items
      .toList
  }

  "Delete data model instances" should "work with multiple externalIds" in {
    val toDeletes = toCreates.map(_.externalId)

    blueFieldClient.nodes.deleteItems(toDeletes, space).unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModel.externalId), space)
    val outputNoFilter = blueFieldClient.nodes
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  it should "ignore unknown externalId" in {
    noException should be thrownBy blueFieldClient.nodes
      .deleteItems(Seq("toto"), space)
      .unsafeRunSync()
  }

  it should "cast any instance property if property definition is a string" in {
    val dataModelForCastingTest = DataModel(
      "cast-test-model-1",
      Some(
        Map(
          "prop_int_as_string" -> dataPropInt,
          "prop_string_as_int" -> dataPropString
        )
      )
    )
    blueFieldClient.dataModels
      .createItems(Seq(dataModelForCastingTest), space)
      .unsafeRunSync()
    val instance =
      Node(
        "i_1",
        properties = Some(
          Map(
            "prop_int_as_string" -> PropertyType.Text.Property("101"),
            "prop_string_as_int" -> PropertyType.Int.Property(102)
          )
        )
      )
    val dataModelInstances = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModelForCastingTest.externalId), items = Seq(instance))
      .unsafeRunSync()
      .toList
    val instancePropsValueMap = dataModelInstances
      .find(_.externalId === instance.externalId)
      .map(_.allProperties)
      .getOrElse(Map.empty)

    dataModelInstances.size shouldBe 1
    instancePropsValueMap.get("prop_int_as_string") shouldBe Some(PropertyType.Int.Property(101))
    instancePropsValueMap.get("prop_string_as_int") shouldBe Some(PropertyType.Text.Property("102"))
  }

  it should "cast any array instance property if property definition is an array of string" in {
    val dataModelArrayForCastingTest = DataModel(
      "cast-array-test-model-1",
      Some(
        Map(
          "prop_ints_as_strings" -> dataPropArrayInt,
          "prop_strings_as_ints" -> dataPropArrayString
        )
      )
    )
    blueFieldClient.dataModels
      .createItems(Seq(dataModelArrayForCastingTest), space)
      .unsafeRunSync()
    val arrayInstance =
      Node(
        "i_2",
        properties = Some(
          Map(
            "prop_ints_as_strings" -> PropertyType.Array.Text.Property(Seq("201", "202")),
            "prop_strings_as_ints" -> PropertyType.Array.Int.Property(Seq(301, 302))
          )
        )
      )
    val arrayDataModelInstances = blueFieldClient.nodes
      .createItems(space, DataModelIdentifier(Some(space), dataModelArrayForCastingTest.externalId), items = Seq(arrayInstance))
      .unsafeRunSync()
      .toList
    val instanceArrayPropValueMap = arrayDataModelInstances
      .find(_.externalId === arrayInstance.externalId)
      .map(_.allProperties)
      .getOrElse(Map.empty)

    arrayDataModelInstances.size shouldBe 1
    instanceArrayPropValueMap.get("prop_ints_as_strings") shouldBe Some(PropertyType.Array.Int.Property(Seq(201, 202)))
    instanceArrayPropValueMap.get("prop_strings_as_ints") shouldBe Some(PropertyType.Array.Text.Property(Seq("301", "302")))
  }

}
