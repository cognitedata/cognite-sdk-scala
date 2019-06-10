package com.cognite.sdk.scala.common

import java.util.UUID

import com.cognite.sdk.scala.v1_0.{RawDatabase, RawDatabases}
import com.softwaremill.sttp.Id

trait RawResourceBehaviors extends SdkTest {
  def withRawDatabase(testCode: String => Any): Unit
  def withRawDatabaseTable(testCode: String => Any): Unit

  def rawDatabaseResource(rawDatabase: RawDatabases[Id]): Unit = {
    val databaseName1 = s"raw-database-test1-${UUID.randomUUID().toString}"
    val databaseName2 = s"raw-database-test2-${UUID.randomUUID().toString}"
    it should "allow creation of databases" in {
      rawDatabase.create(Seq(RawDatabase(databaseName1), RawDatabase(databaseName2)))
      val fetched = rawDatabase.retrieveByIds(Seq(databaseName1, databaseName2)).unsafeBody
      fetched
    }
    it should "allow deletion of databases" in {

    }
    // test attempted delete of non-empty database
  }
//  def rawResource(rows: Seq[RawRow]): Unit = {
//    it should "allow creation and deletion of databases" in {
//
//    }
//    it should "be possible to insert" in withRawDatabase { database =>
//
//    }
//  }
}
