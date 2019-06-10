package com.cognite.sdk.scala.v1_0

import java.util.UUID

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}
import io.circe.syntax._

class RawTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new Client()
  private val idsThatDoNotExist = Seq("nodatabase", "randomdatabase")

  it should "read rows" in {
    val rows = client.rawRows("dots", "with_dots").readAll().toSeq.flatten
    rows.foreach { row =>
      println(row) // scalastyle:ignore
    }
  }

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

  def withDatabase(testCode: String => Any): Unit = {
    val databaseId = client.rawDatabases.create(
      Seq(RawDatabase(name = s"raw-test-${UUID.randomUUID().toString}"))
    ).unsafeBody.head.id
    try {
      val _ = testCode(databaseId)
    } finally {
      val response = client.rawDatabases.deleteByIds(Seq(databaseId))
      val _ = assert(response.isSuccess === true)
    }
  }

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
    val tablesResponse = client.rawTables(database).readAll().flatMap(_.toList)
    assert(tablesResponse.size === tables.size)

    // scalastyle:off
    println("---")
    tablesResponse.foreach(t => println(s"${database} ${t}"))
    tables.foreach(println)
    println("+++")
    // scalastyle:on
    val deleteResponse = client.rawTables(database).deleteByIds(tables.take(1))
    assert(deleteResponse.isSuccess === true)

    val tablesResponseAfterDelete = client.rawTables(database).readAll().flatMap(_.toList)
    assert(tablesResponseAfterDelete.size === tables.size - 1)
  }

  it should "allow creation and deletion of rows" in withDatabaseTables { (database, tables) =>
    val table = tables.head
    val rows = client.rawRows(database, table)
    println("first read")
    val rowsResponse = rows.readAll().flatMap(_.toList)
    assert(rowsResponse.isEmpty)
    println("read complete, now create")
    val createResponse = rows.create(Seq(
      RawRow("123", Map("abc" -> "foo".asJson)),
      RawRow("abc", Map("abc" -> Map("cde" -> 1).asJson))
    ))
    assert(createResponse.isSuccess === true)
    println("create done") // scalastyle:ignore
    val rowsResponseAfterCreate = rows.readAll().flatMap(_.toList).toList
    println("read done") // scalastyle:ignore
    assert(rowsResponseAfterCreate.size === 2)
    assert(rowsResponseAfterCreate.head.key === "123")
    assert(rowsResponseAfterCreate(1).key === "abc")
    assert(rowsResponseAfterCreate.head.columns.keys.size === 1)
    assert(rowsResponseAfterCreate.head.columns.keys.head === "abc")
    assert(rowsResponseAfterCreate.head.columns("abc").as[String].right.get === "foo")
    assert(rowsResponseAfterCreate(1).columns.keys.head === "abc")
    assert(rowsResponseAfterCreate(1).columns("abc").as[Map[String, Int]].right.get === Map("cde" -> 1))

    val deleteResponse = rows.deleteByIds(Seq("123"))
    println("delete done") // scalastyle:ignore
    assert(deleteResponse.isSuccess === true)
    val rowsResponseAfterOneDelete = rows.readAll().flatMap(_.toList).toList
    println("read after delete done") // scalastyle:ignore
    assert(rowsResponseAfterOneDelete.size === 1)
  }

  it should "list all databases and tables" in {
    val databases = client.rawDatabases.readAll().toSeq.flatten
    databases.foreach { database =>
      println(database) // scalastyle:ignore
      val tables = client.rawTables(database.name).readAll().toSeq.flatten
      tables.foreach(t => println(s"  ${t}")) // scalastyle:ignore
    }
  }
}
