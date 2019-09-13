package com.cognite.sdk.scala.v1

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.SdkTest
import io.circe.syntax._
import org.scalatest.ParallelTestExecution

class SequenceRowsTest extends SdkTest with ParallelTestExecution {
  def withSequenceId(testCode: Sequence => Any): Unit = {
    val externalId = shortRandom()
    val sequence = client.sequences.createOneFromRead(
      Sequence(
        name = Some(s"sequence-rows-test-$externalId"),
        externalId = Some(externalId),
        columns =
          NonEmptyList.of(
            SequenceColumn(name = Some("string-column"), externalId = "ext1"),
            SequenceColumn(name = Some("long-column"), externalId = "ext2", valueType = "LONG")
          )
      )
    )
    try {
      val _ = testCode(sequence)
    } finally {
      client.sequences.deleteById(sequence.id)
    }
  }

  private val testRows = Seq(
    SequenceRow(0, Seq("string-0".asJson, 0L.asJson)),
    SequenceRow(1, Seq("string-1".asJson, 1L.asJson))
  )
  private val minRow = testRows.map(_.rowNumber).min
  private val maxRow = testRows.map(_.rowNumber).max

  it should "be possible to insert, update, and delete sequence rows" in withSequenceId { sequence =>
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId).toList, testRows)
    Thread.sleep(5000)
    val (_, rows) = client.sequenceRows.queryById(
      sequence.id, minRow, maxRow + 1)
    rows should contain theSameElementsAs testRows

    val updateRows = testRows.map { row =>
      row.copy(values = row.values.updated(0, row.values.head.mapString(s => s"${s}-updated")))
    }
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId).toList, updateRows)
    Thread.sleep(5000)
    val (_, rowsAfterUpdate) = client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)
    rowsAfterUpdate should contain theSameElementsAs updateRows

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber).take(1))
    Thread.sleep(5000)
    val (_, rowsAfterOneDelete) = client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)
    rowsAfterOneDelete should have size testRows.size.toLong - 1

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber))
    Thread.sleep(5000)
    val (_, rowsAfterDeleteAll) = client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)
    rowsAfterDeleteAll shouldBe empty
  }

  it should "be possible to insert, update and delete sequence rows using externalId" in withSequenceId { sequence =>
    val externalId = sequence.externalId.get
    client.sequenceRows.insertByExternalId(externalId, sequence.columns.map(_.externalId).toList, testRows)
    Thread.sleep(5000)
    val (_, rows) = client.sequenceRows.queryByExternalId(externalId, minRow, maxRow + 1)
    rows should contain theSameElementsAs testRows

    // note: intentionally using queryById here.
    val (_, rowsById) = client.sequenceRows.queryById(
      sequence.id, minRow, maxRow + 1)
    rowsById should contain theSameElementsAs testRows

    client.sequenceRows.deleteByExternalId(externalId, rows.map(_.rowNumber))
    Thread.sleep(5000)
    val (_, rowsAfterDeleteAll) = client.sequenceRows.queryByExternalId(externalId, minRow, maxRow + 1)
    rowsAfterDeleteAll shouldBe empty
  }
}
