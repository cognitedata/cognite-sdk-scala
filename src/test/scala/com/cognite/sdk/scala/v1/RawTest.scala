// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.syntax.either._
import com.cognite.sdk.scala.common.{Items, ReadBehaviours, SdkTestSpec, WritableBehaviors}
import fs2.Stream
import io.circe.Json
import io.circe.syntax._
import org.scalatest.OptionValues
import sttp.client3.UriContext

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.SizeIs"
  )
)
class RawTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with OptionValues {
  private val idsThatDoNotExist = Seq("nodatabase", "randomdatabase")

  it should behave like readable(client.rawDatabases)
  it should behave like writable(
    client.rawDatabases,
    Some(client.rawDatabases),
    Seq(
      RawDatabase(name = s"scala-sdk-${shortRandom()}"),
      RawDatabase(name = s"scala-sdk-${shortRandom()}")
    ),
    Seq(
      RawDatabase(name = s"scala-sdk-${shortRandom()}"),
      RawDatabase(name = s"scala-sdk-${shortRandom()}")
    ),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )

  def withDatabaseTables(testCode: (String, Seq[String]) => Any): Unit = {
    val database = s"raw-test-${shortRandom()}"
    client.rawDatabases.create(Seq(RawDatabase(name = database))).unsafeRunSync()
    val tables = client
      .rawTables(database)
      .create(
        Seq(
          RawTable(name = s"raw-test-${shortRandom()}"),
          RawTable(name = s"raw-test-${shortRandom()}")
        )
      )
      .unsafeRunSync()
      .map(_.id)
    try {
      val _ = testCode(database, tables)
    } catch {
      case t: Throwable => throw t
    } finally {
      try {
        client.rawTables(database).deleteByIds(tables).unsafeRunSync()
      } finally {
        client.rawDatabases.deleteByIds(Seq(database)).unsafeRunSync()
      }
    }
  }

  it should "allow creation and deletion of database tables" in withDatabaseTables {
    (database, tables) =>
      val tablesResponse = client.rawTables(database).list().compile.toList.unsafeRunSync()
      assert(tablesResponse.size === tables.size)

      client.rawTables(database).deleteByIds(tables.take(1)).unsafeRunSync()

      val tablesResponseAfterDelete = client.rawTables(database).list().compile.toList.unsafeRunSync()
      assert(tablesResponseAfterDelete.size === tables.size - 1)

      client.rawTables(database).create(Seq(RawTable(name = tables.head))).unsafeRunSync()

      val tablesResponseAfterCreate = client.rawTables(database).list().compile.toList.unsafeRunSync()
      assert(tablesResponseAfterCreate.size === tables.size)
  }

  it should "allow creation and deletion of rows" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val rows = client.rawRows(database, table)

    val rowsResponse = rows.list().compile.toList.unsafeRunSync()
    assert(rowsResponse.isEmpty)

    rows.create(
      Seq(
        RawRow("123", Map("abc" -> "foo".asJson)),
        RawRow("abc", Map("abc" -> Map("cde" -> 1).asJson))
      )
    ).unsafeRunSync()

    // we need cats.syntax.either._ to make this backwards compatible with Scala 2.11
    // while avoiding deprecation warnings on Scala 2.13, which does not need that import.
    // use it for some nonsense here to make the import "used" also for Scala 2.13
    val either: Either[String, String] = Either.right("asdf")
    assert(either.forall(_ === "asdf"))

    val rowsResponseAfterCreate = rows.list().compile.toList.unsafeRunSync()
    assert(rowsResponseAfterCreate.size === 2)
    assert(rowsResponseAfterCreate.head.key === "123")
    assert(rowsResponseAfterCreate(1).key === "abc")
    assert(rowsResponseAfterCreate.head.columns.keys.size === 1)
    assert(rowsResponseAfterCreate.head.columns.keys.head === "abc")
    assert(rowsResponseAfterCreate.head.columns("abc").as[String].forall(_ === "foo"))
    assert(rowsResponseAfterCreate(1).columns.keys.head === "abc")
    assert(
      rowsResponseAfterCreate(1).columns("abc").as[Map[String, Int]].forall(_ === Map("cde" -> 1))
    )

    rows.deleteByIds(Seq("123")).unsafeRunSync()
    val rowsResponseAfterOneDelete = rows.list().compile.toList.unsafeRunSync()
    assert(rowsResponseAfterOneDelete.size === 1)

    rows.createOne(RawRow("1b3", Map("abc" -> "cdf".asJson))).unsafeRunSync()
    val rowsResponseAfterCreateOne = rows.list().compile.toList.unsafeRunSync()
    assert(rowsResponseAfterCreateOne.size === 2)
  }

  it should "ensure parent" in {
    val database = s"raw-test-ensureParent-${shortRandom()}"
    val table =  s"raw-test-${shortRandom()}"
    try {
      client.rawRows(database, table).createItems(Items(Seq(RawRow("something", Map()))), ensureParent = true).unsafeRunSync()
      assert(client.rawTables(database).list().compile.toList.unsafeRunSync().map(_.id).contains(table))
    } finally {
      try {
        client.rawTables(database).deleteByIds(Seq(table)).unsafeRunSync()
      } finally {
        client.rawDatabases.deleteByIds(Seq(database)).unsafeRunSync()
      }
    }
  }

  it should "allow controlling filtering of field with null value" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val rows = client.rawRows(database, table, filterNullFields = true)

    val relevantKeys = List("withNullFields", "normal")
    rows.create(
      Seq(
        RawRow(relevantKeys.head, Map("a" -> "3".asJson, "notthere" -> None.asJson)),
        RawRow(relevantKeys.last, Map("a" -> "0".asJson, "abc" -> "".asJson))
      )
    ).unsafeRunSync()

    // We cannot test using a key filter here, as this is translated into a getRowByKey request, which does not
    // currently support filtering out null fields.

    val listedRowsWithNullFilter: Map[String, Map[String, Json]] = rows.list()
      .compile
      .toList
      .unsafeRunSync()
      .filter(row => relevantKeys.contains(row.key)).map(r => (r.key -> r.columns))
      .toMap

    assert(listedRowsWithNullFilter.size == 2)
    val values = listedRowsWithNullFilter.get("withNullFields").toList.head
    assert(values.size == 1)
    values.get("a").toList.head shouldBe "3".asJson
    assert(listedRowsWithNullFilter.get("normal").toList.head.size == 2)

    val filteredWithNullFilter: Map[String, Map[String, Json]] =
      rows.filterWithCursor(RawRowFilter(), None, None, None)
        .unsafeRunSync()
        .items
        .filter(row => relevantKeys.contains(row.key))
        .map(r => (r.key -> r.columns))
        .toMap

    assert(filteredWithNullFilter.size == 2)
    val filteredValues = filteredWithNullFilter.get("withNullFields").toList.head
    assert(filteredValues.size == 1)
    filteredValues.get("a").toList.head shouldBe "3".asJson
    assert(listedRowsWithNullFilter.get("normal").toList.head.size == 2)
  }

  it should "add correct option when filtering out null fields" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val unfilteredRows = client.rawRows(database, table)
    val filteredRows = client.rawRows(database, table, filterNullFields = true)

    val modifiedFilteredUrl = filteredRows.filterFieldsWithNull(uri"http://localhost/testQuery")
    assert(modifiedFilteredUrl.params.toMap.get("filterNullFields").contains("true"))

    val modifiedUnfilteredUrl = unfilteredRows.filterFieldsWithNull(uri"http://localhost/testQuery")
    assert(modifiedUnfilteredUrl.params.toMap.get("filterNullFields").contains("false"))
  }


  it should "allow partition read and filtering of rows" in withDatabaseTables {
    (database, tables) =>
      val rows = client
        .rawRows(database, tables.head)

      rows.create(
        Seq(
          RawRow("123", Map("a" -> "3".asJson, "abc" -> "foo".asJson)),
          RawRow("abc", Map("a" -> "0".asJson, "abc" -> Map("cde" -> 1).asJson))
        )
      ).unsafeRunSync()

      val partitions = rows.filterPartitionsF(RawRowFilter(), 20, Some(10)).unsafeRunSync()
      assert(partitions.length === 20)
      assert(partitions.fold(Stream.empty)(_ ++ _).compile.toVector.unsafeRunSync().nonEmpty)

      val columns = rows
          .filter(RawRowFilter(columns = Some(Seq("abc"))))
          .compile
          .toList
          .unsafeRunSync()
          .head
          .columns
          .keySet
      columns should contain only "abc"

      val prevRows = rows.list().compile.toList.unsafeRunSync()
      rows.create(
        Seq(
          RawRow("a", Map("a" -> "3".asJson, "abc" -> "foo".asJson)),
          RawRow("c", Map("a" -> "3".asJson, "abc" -> "foo".asJson))
        )
      ).unsafeRunSync()
      val currentRows = rows.list().compile.toList.unsafeRunSync()
      assert(currentRows.length === 4)

      val maxLastUpdatedTime = prevRows.map(_.lastUpdatedTime.value).max
      assert(
        rows
          .filter(RawRowFilter(minLastUpdatedTime = Some(maxLastUpdatedTime)))
          .compile
          .toList
          .unsafeRunSync()
          .length === 2
      )

      assert(
        rows
          .filter(RawRowFilter(maxLastUpdatedTime = Some(maxLastUpdatedTime.plusMillis(1))))
          .compile
          .toList
          .unsafeRunSync()
          .length === 2
      )

      rows.retrieveByKey("abc").unsafeRunSync() match {
        case RawRow(key, columns, _) => {
          assert(key === "abc")
          assert(columns === Map("a" -> "0".asJson, "abc" -> Map("cde" -> 1).asJson))
          }
      }
  }

  it should "get partition cursor" in withDatabaseTables {
    (database, tables) =>
      val rows = client
        .rawRows(database, tables.head)

      rows.create(
        Seq(
          RawRow("123", Map("a" -> "3".asJson, "abc" -> "foo".asJson)),
          RawRow("abc", Map("a" -> "0".asJson, "abc" -> Map("cde" -> 1).asJson))
        )
      ).unsafeRunSync()

      val pCursors = rows.getPartitionCursors(RawRowFilter(), 15).unsafeRunSync()
      pCursors.size shouldBe 15

  }
}
