// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.{CdpApiException, Items, RetryWhile}
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
    DataModelInstanceCreate(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> StringProperty("equipment_43"),
          "prop_string" -> StringProperty("EQ0001"),
          "prop_float" -> Float32Property(0.1f),
          "prop_direct_relation" -> DirectRelationProperty("Asset"),
          "prop_date" -> DateProperty(LocalDate.of(2022, 3, 22))
        )
      )
    )

  val dataModelInstanceToCreate2 =
    DataModelInstanceCreate(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> StringProperty("equipment_44"),
          "prop_string" -> StringProperty("EQ0002"),
          "prop_bool" -> BooleanProperty(true),
          "prop_float" -> Float32Property(1.64f)
        )
      )
    )

  val dataModelInstanceToCreate3 =
    DataModelInstanceCreate(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> StringProperty("equipment_45"),
          "prop_string" -> StringProperty("EQ0011"),
          "prop_bool" -> BooleanProperty(false),
          "prop_float" -> Float32Property(3.5f)
        )
      )
    )

  val toCreates =
    Seq(dataModelInstanceToCreate1, dataModelInstanceToCreate2, dataModelInstanceToCreate3)

  val dataPropArrayString = DataModelPropertyDeffinition(PropertyType.ArrayText, true)
  // val dataPropArrayFloat = DataModelProperty(PropertyName.arrayFloat32, false) //float[] is not supported yet
  val dataPropArrayInt = DataModelPropertyDeffinition(PropertyType.ArrayInt, true)

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

  val dmiArrayToCreate1 = DataModelInstanceCreate(
    dataModelArray.externalId,
    Some(
      Map(
        "externalId" -> StringProperty("equipment_42"),
        "array_string" -> ArrayProperty[StringProperty](
          Vector(
            StringProperty("E101"),
            StringProperty("E102"),
            StringProperty("E103")
          )
        ),
        /*"array_float" -> ArrayProperty[Float32Property](
          Vector(
            Float32Property(1.01f),
            Float32Property(1.02f)
          )
        ),*/ // float[] is not supported yet
        "array_int" -> ArrayProperty[Int32Property](
          Vector(
            Int32Property(1),
            Int32Property(12),
            Int32Property(13)
          )
        )
      )
    )
  )
  val dmiArrayToCreate2 = DataModelInstanceCreate(
    dataModelArray.externalId,
    Some(
      Map(
        "externalId" -> StringProperty("equipment_43"),
        "array_string" -> ArrayProperty(
          Vector(
            StringProperty("E201"),
            StringProperty("E202")
          )
        )
        /*"array_float" -> ArrayProperty(
          Vector(
            Float32Property(2.02f),
            Float32Property(2.04f)
          )
        )*/ // float[] is not supported yet
      )
    )
  )
  val dmiArrayToCreate3 = DataModelInstanceCreate(
    dataModelArray.externalId,
    Some(
      Map(
        "externalId" -> StringProperty("equipment_44"),
        /*"array_float" -> ArrayProperty(
          Vector(
            Float32Property(3.01f),
            Float32Property(3.02f)
          )
        ),*/ // float[] is not supported yet
        "array_int" -> ArrayProperty(
          Vector(
            Int32Property(3),
            Int32Property(12),
            Int32Property(13)
          )
        )
      )
    )
  )

  val dmiArrayToCreates =
    Seq(dmiArrayToCreate1, dmiArrayToCreate2, dmiArrayToCreate3)

  override def beforeAll(): Unit = {
    blueFieldClient.dataModels
      .createItems(Items[DataModel](Seq(dataModel, dataModelArray)))
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list().unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe true
    )
    ()
  }

  override def afterAll(): Unit = {
    blueFieldClient.dataModels
      .deleteItems(Seq(dataModel.externalId, dataModelArray.externalId))
      .unsafeRunSync()

    retryWithExpectedResult[scala.Seq[DataModel]](
      blueFieldClient.dataModels.list().unsafeRunSync(),
      dm => dm.contains(dataModel) && dm.contains(dataModelArray) shouldBe false
    )
    ()
  }

  "Insert data model instances" should "work with multiple input" in {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(Items[DataModelInstanceCreate](toCreates))
      .unsafeRunSync()
      .toList

    dataModelInstances.size shouldBe 3
    dataModelInstances.map(_.modelExternalId).toSet shouldBe toCreates.map(_.modelExternalId).toSet
  }

  it should "fail if input data type is not correct" in {
    val invalidInput = DataModelInstanceCreate(
      dataModel.externalId,
      Some(
        Map(
          "externalId" -> StringProperty("equipment_47"),
          "prop_float" -> StringProperty("abc")
        )
      )
    )
    val exception = the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstanceCreate](
          Seq(invalidInput)
        )
      )
      .unsafeRunSync()

    exception.message.contains("invalid input") shouldBe true
    exception.message.contains("abc") shouldBe true
  }

  private def insertDMIBeforeQuery() = {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstanceCreate](
          toCreates
        )
      )
      .unsafeRunSync()
      .toList
    dataModelInstances.size shouldBe 3
    dataModelInstances
  }

  private def deleteDMIAfterQuery() = {
    val toDeletes =
      toCreates
        .flatMap(_.properties)
        .flatMap(_.get("externalId").collect { case StringProperty(id) =>
          id
        })
    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(dataModel.externalId)
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  private def initAndCleanUpDataForQuery(testCode: Seq[DataModelInstanceCreate] => Any): Unit =
    try {
      val dataModelInstances = insertDMIBeforeQuery()
      val _ = testCode(dataModelInstances)
    } catch {
      case t: Throwable => throw t
    } finally {
      deleteDMIAfterQuery()
      ()
    }

  private def fromCreatedToExpectedProps(instances: Set[DataModelInstanceCreate]) =
    instances.map(_.properties)

  "Query data model instances" should "work with empty filter" in initAndCleanUpDataForQuery { _ =>
    val inputNoFilterQuery = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ0001"))
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
      dataModel.externalId,
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ0002")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), BooleanProperty(true)),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_float"), Float32Property(1.64f))
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

    outputQueryAnd.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2)
    )

    val inputQueryAnd2 = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIAndFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ0001")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), BooleanProperty(true))
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
      dataModel.externalId,
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), BooleanProperty(true))
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
    outputQueryOr.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  it should "work with NOT filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryNot = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMINotFilter(
          DMIInFilter(
            Seq(dataModel.externalId, "prop_string"),
            Seq(StringProperty("EQ0002"), StringProperty("EQ0011"))
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
    outputQueryNot.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate1)
    )
  }

  it should "work with PREFIX filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIPrefixFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ000"))
      )
    )
    val outputQueryPrefix = blueFieldClient.dataModelInstances
      .query(inputQueryPrefix)
      .unsafeRunSync()
      .items
      .toList

    outputQueryPrefix.size shouldBe 2
    outputQueryPrefix.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate1, dataModelInstanceToCreate2)
    )
  }

  it should "work with RANGE filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryRange = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIRangeFilter(
          Seq(dataModel.externalId, "prop_float"),
          gte = Some(Float32Property(1.64f))
        )
      )
    )
    val outputQueryRange = blueFieldClient.dataModelInstances
      .query(inputQueryRange)
      .unsafeRunSync()
      .items
      .toList

    outputQueryRange.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  it should "work with EXISTS filter" in initAndCleanUpDataForQuery { _ =>
    val inputQueryExists = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIExistsFilter(Seq(dataModel.externalId, "prop_bool"))
      )
    )
    val outputQueryExists = blueFieldClient.dataModelInstances
      .query(inputQueryExists)
      .unsafeRunSync()
      .items
      .toList

    outputQueryExists.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
      Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3)
    )
  }

  private def insertDMIArrayBeforeQuery() = {
    val dataModelInstances = blueFieldClient.dataModelInstances
      .createItems(
        Items[DataModelInstanceCreate](
          dmiArrayToCreates
        )
      )
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
        .collect { case StringProperty(id) =>
          id
        }
    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(dataModelArray.externalId)
    val outputNoFilter = blueFieldClient.dataModelInstances
      .query(inputNoFilterQuery)
      .unsafeRunSync()
      .items
      .toList
    outputNoFilter.isEmpty shouldBe true
  }

  private def initAndCleanUpArrayDataForQuery(testCode: Seq[DataModelInstanceCreate] => Any): Unit =
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
      dataModelArray.externalId,
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_string"),
          Seq(
            StringProperty("E201"),
            StringProperty("E103")
          )
        )
      )
    )
    val outputQueryContainsAnyString = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAnyString)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAnyString.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
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
      dataModelArray.externalId,
      Some(
        DMIContainsAnyFilter(
          Seq(dataModelArray.externalId, "array_string"),
          Seq(
            StringProperty("E201"),
            StringProperty("E202")
          )
        )
      )
    )
    val outputQueryContainsAllString = blueFieldClient.dataModelInstances
      .query(inputQueryContainsAllString)
      .unsafeRunSync()
      .items
      .toList

    outputQueryContainsAllString.map(_.properties).toSet shouldBe fromCreatedToExpectedProps(
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
      dataModel.externalId,
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

    outputQueryExists.map(_.properties) shouldBe Seq(
      dataModelInstanceToCreate3,
      dataModelInstanceToCreate2,
      dataModelInstanceToCreate1
    ).map(_.properties)
  }

  it should "work with limit" in initAndCleanUpDataForQuery { _ =>
    val inputQueryOr = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIOrFilter(
          Seq(
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ0011")),
            DMIEqualsFilter(Seq(dataModel.externalId, "prop_bool"), BooleanProperty(true))
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
    val expected: Set[Option[Map[String, DataModelProperty]]] =
      fromCreatedToExpectedProps(Set(dataModelInstanceToCreate2, dataModelInstanceToCreate3))

    outputQueryOr
      .map(_.properties)
      .toSet
      .subsetOf(expected) shouldBe true
  }

  it should "work with cursor and stream" in initAndCleanUpDataForQuery { _ =>
    val inputQueryPrefix = DataModelInstanceQuery(
      dataModel.externalId,
      Some(
        DMIPrefixFilter(Seq(dataModel.externalId, "prop_string"), StringProperty("EQ00"))
      )
    )

    def checkOutputProp(output: Seq[DataModelInstanceQueryResponse]): Assertion = {
      val expected = fromCreatedToExpectedProps(toCreates.toSet)
      output
        .map(_.properties)
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
    val toGets = toCreates.map { d =>
      DataModelInstanceByExternalId(
        d.properties
          .flatMap(_.get("externalId"))
          .collect { case StringProperty(id) =>
            id
          }
          .getOrElse(""),
        d.modelExternalId
      )
    }
    val outputList = blueFieldClient.dataModelInstances
      .retrieveByExternalIds(toGets, false)
      .unsafeRunSync()
      .toList
    outputList.size shouldBe 3
    outputList.map(_.properties).toSet shouldBe toCreates.map(_.properties).toSet
  }

  // Not yet supported
  ignore should "raise an exception if input has invalid externalId and ignoreUnknownIds is false" in {
    the[CdpApiException] thrownBy blueFieldClient.dataModelInstances
      .retrieveByExternalIds(
        Seq(DataModelInstanceByExternalId(dataModel.externalId, "toto")),
        false
      )
      .unsafeRunSync()
      .toList
  }

  // Not yet supported
  ignore should "ignore if input has invalid externalId and ignoreUnknownIds is true" in {
    val res = blueFieldClient.dataModelInstances
      .retrieveByExternalIds(
        Seq(DataModelInstanceByExternalId(dataModel.externalId, "toto")),
        true
      )
      .unsafeRunSync()
      .toList

    res.isEmpty shouldBe true
  }

  "Delete data model instances" should "work with multiple externalIds" in {
    val toDeletes =
      toCreates
        .flatMap(_.properties)
        .map(
          _.get("externalId")
            .collect { case StringProperty(id) =>
              id
            }
            .getOrElse("")
        )

    blueFieldClient.dataModelInstances
      .deleteByExternalIds(toDeletes)
      .unsafeRunSync()

    // make sure that data is deleted
    val inputNoFilterQuery = DataModelInstanceQuery(dataModel.externalId)
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
