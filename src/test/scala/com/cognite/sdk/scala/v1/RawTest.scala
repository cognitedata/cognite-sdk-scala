package com.cognite.sdk.scala.v1

import java.util.UUID

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}
import io.circe.syntax._

class RawTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()
  private val idsThatDoNotExist = Seq("nodatabase", "randomdatabase")

  private def shortRandom() = UUID.randomUUID().toString.substring(0, 8)
  it should behave like readableResource(client.rawDatabases)
  it should behave like writableResource(
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
    val databaseCreateResponse = client.rawDatabases.create(Seq(RawDatabase(name = database)))
    databaseCreateResponse.isSuccess should be (true)
    val tables = client.rawTables(database).create(
      Seq(
        RawTable(name = s"raw-test-${shortRandom()}"),
        RawTable(name = s"raw-test-${shortRandom()}"))
    ).unsafeBody.map(_.id)
    try {
      val _ = testCode(database, tables)
    } finally {
      try {
        val response = client.rawTables(database).deleteByIds(tables)
        val _ = assert(response.isSuccess === true)
      } finally {
        val response = client.rawDatabases.deleteByIds(Seq(database))
        val _ = assert(response.isSuccess === true)
      }
    }
  }

  it should "allow creation and deletion of database tables" in withDatabaseTables { (database, tables) =>
    val tablesResponse = client.rawTables(database).readAll().flatMap(_.unsafeBody.toList).toList
    assert(tablesResponse.size === tables.size)

    val deleteResponse = client.rawTables(database).deleteByIds(tables.take(1))
    assert(deleteResponse.isSuccess === true)

    val tablesResponseAfterDelete = client.rawTables(database).readAll().flatMap(_.unsafeBody.toList).toList
    assert(tablesResponseAfterDelete.size === tables.size - 1)

    val tablesCreateResponse = client.rawTables(database).create(Seq(RawTable(name = tables.head)))
    assert(tablesCreateResponse.isSuccess === true)

    val tablesResponseAfterCreate = client.rawTables(database).readAll().flatMap(_.unsafeBody.toList).toList
    assert(tablesResponseAfterCreate.size === tables.size)
  }

  it should "allow creation and deletion of rows" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val rows = client.rawRows(database, table)

    val rowsResponse = rows.readAll().flatMap(_.unsafeBody.toList).toList
    assert(rowsResponse.isEmpty)

    val createResponse = rows.create(Seq(
      RawRow("123", Map("abc" -> "foo".asJson)),
      RawRow("abc", Map("abc" -> Map("cde" -> 1).asJson))
    ))
    assert(createResponse.isSuccess === true)

    val rowsResponseAfterCreate = rows.readAll().flatMap(_.unsafeBody.toList).toList
    assert(rowsResponseAfterCreate.size === 2)
    assert(rowsResponseAfterCreate.head.key === "123")
    assert(rowsResponseAfterCreate(1).key === "abc")
    assert(rowsResponseAfterCreate.head.columns.keys.size === 1)
    assert(rowsResponseAfterCreate.head.columns.keys.head === "abc")
    assert(rowsResponseAfterCreate.head.columns("abc").as[String].right.get === "foo")
    assert(rowsResponseAfterCreate(1).columns.keys.head === "abc")
    assert(rowsResponseAfterCreate(1).columns("abc").as[Map[String, Int]].right.get === Map("cde" -> 1))

    val deleteResponse = rows.deleteByIds(Seq("123"))
    assert(deleteResponse.isSuccess === true)
    val rowsResponseAfterOneDelete = rows.readAll().flatMap(_.unsafeBody.toList).toList
    assert(rowsResponseAfterOneDelete.size === 1)
  }
  // scalastyle:off
//  it should "list all databases and tables" in {
//    val databases = client.rawDatabases.readAll().flatMap(_.unsafeBody.toList).toList
//    databases.foreach { database =>
//      println(database)
//      val tables = client.rawTables(database.name).readAll().flatMap(_.unsafeBody.toList).toList
//      tables.foreach(t => println(s"  ${t}")) // scalastyle:ignore
//    }
//  }
}
