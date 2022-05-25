// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{CdpApiException, RetryWhile}
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
    "org.wartremover.warts.Serializable"
  )
)
class DataModelInstancesTest
    extends CommonDataModelTestHelper
    with RetryWhile
    with BeforeAndAfterAll {
  val uuid = UUID.randomUUID.toString
  val dataPropString = DataModelPropertyDeffinition(PropertyType.Text)
  val dataPropBool = DataModelPropertyDeffinition(PropertyType.Boolean)
  val dataPropFloat = DataModelPropertyDeffinition(PropertyType.Float32, nullable = false)
  val dataPropDirectRelation = DataModelPropertyDeffinition(PropertyType.DirectRelation)
  val dataPropDate = DataModelPropertyDeffinition(PropertyType.Date)

  val dataModel = DataModel(
    s"Equipment-${uuid.substring(0, 8)}",
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

  val dataModelInstanceToCreate1 =
    Node("equipment_43",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0001"),
          "prop_float" -> PropertyType.Float32.Property(0.1f),
          "prop_direct_relation" -> PropertyType.DirectRelation.Property("Asset"),
          "prop_date" -> PropertyType.Date.Property(LocalDate.of(2022, 3, 22))
        )
      )
    )

  val dataModelInstanceToCreate2 =
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

  val dataModelInstanceToCreate3 =
    Node(
      "equipment_45",
      properties = Some(
        Map(
          "prop_string" -> PropertyType.Text.Property("EQ0011"),
          "prop_bool" -> PropertyType.Boolean.Property(false),
          "prop_float" -> PropertyType.Float32.Property(3.5f)
        )
      )
    )

  val toCreates =
    Seq(dataModelInstanceToCreate1, dataModelInstanceToCreate2, dataModelInstanceToCreate3)

  val dataPropArrayString = DataModelPropertyDeffinition(PropertyType.Array.Text, true)
  // val dataPropArrayFloat = DataModelProperty(PropertyName.arrayFloat32, false) //float[] is not supported yet
  val dataPropArrayInt = DataModelPropertyDeffinition(PropertyType.Array.Int, true)

  val dataModelArray = DataModel(
    s"Equipment-${UUID.randomUUID.toString.substring(0, 8)}",
    Some(
      Map(
        "array_string" -> dataPropArrayString,
        // "array_float" -> dataPropArrayFloat, //float[] is not supported yet
        "array_int" -> dataPropArrayInt
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
  
  private val space = "test-space"

  override def beforeAll(): Unit = {
    blueFieldClient.dataModels
      .createItems(Seq(dataModel, dataModelArray), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe true
    )
    ()
  }

  override def afterAll(): Unit = {
    blueFieldClient.dataModels
      .deleteItems(Seq(dataModel.externalId, dataModelArray.externalId), space)
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list(space).unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe false
    )
    ()
  }

  "Insert data model instances" should "work with multiple input" in {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = toCreates)
      .unsafeRunSync()
      .toList

    dataModelInstances.size shouldBe 3
    dataModelInstances.map(_.externalId).toSet shouldBe toCreates.map(_.externalId).toSet
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
    val exception = the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = Seq(invalidInput))
      .unsafeRunSync()

    exception.message.contains("invalid input") shouldBe true
    exception.message.contains("abc") shouldBe true
  }

  private def insertDMIBeforeQuery() = {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = toCreates)
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIAfterQuery() = {
    val toDeletes =
      toCreates.map(_.externalId)
    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModel.externalId))
    val outputNoFilter = blueFieldClient.dataModelInstances
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

  private def fromCreatedToExpectedProps(instances: Set[PropertyMap]) =
    instances.map(_.allProperties)

  "Query data model instances" should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0001"))
          )
        )
      )
    )
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
    outputNoFilter.items.toList.size shouldBe 1
  }

  it should "work with AND filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryAnd = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0002")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true)),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_float"), PropertyType.Float32.Property(1.64f))
          )
        )
      )
    )
    val outputQueryAnd = blueFieldClient.dataModelInstances
      .query(inputQueryAnd)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAnd.size shouldBe 1

    outputQueryAnd.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2)
    )

    val inputQueryAnd2 = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0001")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
          )
        )
      )
    )
    val outputQueryAndEmpty = blueFieldClient.dataModelInstances
      .query(inputQueryAnd2)
      .unsafeRunSync()
      .items
      .toList

    outputQueryAndEmpty.isEmpty shouldBe true
  }

  it should "work with OR filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
          )
        )
      )
    )
    val outputQueryOr = blueFieldClient.dataModelInstances
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 2
    outputQueryOr.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMINotFilter(
          DMIInFilter(
            Seq(dataModel.externalId, "prop_string"),
            Seq(PropertyType.Text.Property("EQ0002"), PropertyType.Text.Property("EQ0011"))
          )
        )
      )
    )
    val outputQueryNot = blueFieldClient.dataModelInstances
      .query(inputQueryNot)
      .unsafeRunSync()
      .items
      .toList

    outputQueryNot.size shouldBe 1
    outputQueryNot.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate1)
    )
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIPrefixFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ000"))
      )
    )
    val outputQueryPrefix = blueFieldClient.dataModelInstances
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate1, dataModelInstanceToCreate2)
    )
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIRangeFilter(
          Seq(dataModel.externalId, "prop_float"),
          gte = Some(PropertyType.Float32.Property(1.64f))
        )
      )
    )
    val outputQueryRange = blueFieldClient.dataModelInstances
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIExistsFilter(Seq(dataModel.externalId, "prop_bool"))
      )
    )
    val outputQueryExists = blueFieldClient.dataModelInstances
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  private def insertDMIArrayBeforeQuery() = {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(space, DataModelIdentifier(Some(space), dataModel.externalId), items = dmiArrayToCreates)
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIArrayAfterQuery() = {
    val toDeletes =
      dmiArrayToCreates
        .flatMap(_.properties)
        .flatMap(_.get("externalId"))
        .collect { case PropertyType.Text.Property(id) =>
          id
        }
    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModelArray.externalId))
    val outputNoFilter = blueFieldClient.dataModelInstances
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
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_string"),
          Seq(
            PropertyType.Text.Property("E201"),
            PropertyType.Text.Property("E103")
          )
        )
      )
    )
    val outputQueryContainsAnyString = blueFieldClient.dataModelInstances
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
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_string"),
          Seq(
            PropertyType.Text.Property("E201"),
            PropertyType.Text.Property("E202")
          )
        )
      )
    )
    val outputQueryContainsAllString = blueFieldClient.dataModelInstances
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
      Some(
        DMIExistsFilter(Seq(dataModel.externalId, "prop_float"))
      ),
      Some(Seq(dataModel.externalId, "col_float:desc"))
    )
    val outputQueryExists = blueFieldClient.dataModelInstances
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.allProperties) shouldBe Seq(
      dataModelInstanceToCreate3,
      dataModelInstanceToCreate2,
      dataModelInstanceToCreate1
    ).map(_.properties)
  }

  it should "work with limit" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), PropertyType.Boolean.Property(true))
          )
        )
      ),
      None,
      Some(1)
    )
    val outputQueryOr = blueFieldClient.dataModelInstances
      .query(inputQueryOr)
      .unsafeRunSync()
      .items
      .toList

    outputQueryOr.size shouldBe 1
    val expected: Set[Map[String, PropertyType.AnyProperty]] =
      fromCreatedToExpectedProps(Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3))

    outputQueryOr
      .map(_.allProperties)
      .toSet
      .subsetOf(expected) shouldBe true
  }

  it should "work with cursor and stream" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      DataModelIdentifier(Some(space),dataModel.externalId),
      Some(
        DMIPrefixFilter(Seq(dataModel.externalId, "prop_string"), PropertyType.Text.Property("EQ00"))
      )
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

    val outputLimit1 = blueFieldClient.dataModelInstances
      .queryStream(inputQueryPrefix, Some(1))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit1.size shouldBe 1
    checkOutputProp(outputLimit1)

    val outputLimit2 = blueFieldClient.dataModelInstances
      .queryStream(inputQueryPrefix, Some(2))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit2.size shouldBe 2
    checkOutputProp(outputLimit2)

    val outputLimit3 = blueFieldClient.dataModelInstances
      .queryStream(inputQueryPrefix, Some(3))
      .compile
      .toList
      .unsafeRunSync()
    outputLimit3.size shouldBe 3
    checkOutputProp(outputLimit3)
  }

  // Not yet supported
  "List data model instances" should "work with multiple externalIds" ignore {
    val outputList = blueFieldClient.dataModelInstances
      .retrieveByExternalIds(DataModelIdentifier(Some(space), dataModel.externalId), toCreates.map(_.externalId))
      .unsafeRunSync()
      .items
      .toList
    outputList.size shouldBe 3
    outputList.map(_.allProperties).toSet shouldBe toCreates.map(_.properties).toSet
  }

  // Not yet supported
  ignore should "raise an exception if input has invalid externalId" in {
    the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .retrieveByExternalIds(DataModelIdentifier(Some(space), dataModel.externalId), Seq("toto"))
      .unsafeRunSync()
      .items
      .toList
  }

  "Delete data model instances" should "work with multiple externalIds" in {
    val toDeletes =
      toCreates.map(_.externalId)

    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(DataModelIdentifier(Some(space), dataModel.externalId))
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  it should "ignore unknown externalId" in {
    noException should be thrownBy blueFieldClient.dataModelInstances
      .deleteByExternalIds(Seq("toto"))
      .unsafeRunSync()
  }
}
