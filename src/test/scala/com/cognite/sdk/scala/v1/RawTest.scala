package com.cognite.sdk.scala.v1

import cats.{Functor, Id}
import cats.syntax.either._
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}
import io.circe.syntax._

class RawTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(implicitly[Functor[Id]], auth, sttpBackend)
  private val idsThatDoNotExist = Seq("nodatabase", "randomdatabase")

  it should behave like readable(client.rawDatabases)
  it should behave like writable(
      client.rawDatabases,
      Seq(
        RawDatabase(name = s"scala-sdk-${shortRandom()}"),
        RawDatabase(name = s"scala-sdk-${shortRandom()}")),
      Seq(
        RawDatabase(name = s"scala-sdk-${shortRandom()}"),
        RawDatabase(name = s"scala-sdk-${shortRandom()}")),
    idsThatDoNotExist,
    supportsMissingAndThrown = false
  )

  def withDatabaseTables(testCode: (String, Seq[String]) => Any): Unit = {
    val database = s"raw-test-${shortRandom()}"
    client.rawDatabases.create(Seq(RawDatabase(name = database)))
    val tables = client.rawTables(database).create(
      Seq(
        RawTable(name = s"raw-test-${shortRandom()}"),
        RawTable(name = s"raw-test-${shortRandom()}"))
    ).map(_.id)
    try {
      val _ = testCode(database, tables)
    } finally {
      try {
        client.rawTables(database).deleteByIds(tables)
      } finally {
        client.rawDatabases.deleteByIds(Seq(database))
      }
    }
  }

  it should "allow creation and deletion of database tables" in withDatabaseTables { (database, tables) =>
    val tablesResponse = client.rawTables(database).readAll().toList
    assert(tablesResponse.size === tables.size)

    client.rawTables(database).deleteByIds(tables.take(1))

    val tablesResponseAfterDelete = client.rawTables(database).readAll().toList
    assert(tablesResponseAfterDelete.size === tables.size - 1)

    client.rawTables(database).create(Seq(RawTable(name = tables.head)))

    val tablesResponseAfterCreate = client.rawTables(database).readAll().toList
    assert(tablesResponseAfterCreate.size === tables.size)
  }

  it should "allow creation and deletion of rows" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val rows = client.rawRows(database, table)

    val rowsResponse = rows.readAll().toList
    assert(rowsResponse.isEmpty)

    rows.create(Seq(
      RawRow("123", Map("abc" -> "foo".asJson)),
      RawRow("abc", Map("abc" -> Map("cde" -> 1).asJson))
    ))

    // we need cats.syntax.either._ to make this backwards compatible with Scala 2.11
    // while avoiding deprecation warnings on Scala 2.13, which does not need that import.
    // use it for some nonsense here to make the import "used" also for Scala 2.13
    val either: Either[String, String] = Either.right("asdf")
    assert(either.forall(_ == "asdf"))

    val rowsResponseAfterCreate = rows.readAll().toList
    assert(rowsResponseAfterCreate.size === 2)
    assert(rowsResponseAfterCreate.head.key === "123")
    assert(rowsResponseAfterCreate(1).key === "abc")
    assert(rowsResponseAfterCreate.head.columns.keys.size === 1)
    assert(rowsResponseAfterCreate.head.columns.keys.head === "abc")
    assert(rowsResponseAfterCreate.head.columns("abc").as[String].forall(_ === "foo"))
    assert(rowsResponseAfterCreate(1).columns.keys.head === "abc")
    assert(rowsResponseAfterCreate(1).columns("abc").as[Map[String, Int]].forall(_ === Map("cde" -> 1)))

    rows.deleteByIds(Seq("123"))
    val rowsResponseAfterOneDelete = rows.readAll().toList
    assert(rowsResponseAfterOneDelete.size === 1)
  }
}
