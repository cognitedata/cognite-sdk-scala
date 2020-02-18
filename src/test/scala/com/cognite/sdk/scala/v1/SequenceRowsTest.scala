package com.cognite.sdk.scala.v1

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.{RetryWhile, SdkTestSpec}
import io.circe.syntax._
import org.scalatest.ParallelTestExecution

class SequenceRowsTest extends SdkTestSpec with ParallelTestExecution with RetryWhile {
  def withSequence(testCode: Sequence => Any): Unit = {
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
    } catch {
      case t: Throwable => throw t
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

  it should "be possible to insert, update, and delete sequence rows" in withSequence { sequence =>
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId).toList, testRows)
    val rows = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)._2,
      r => r should contain theSameElementsAs testRows
    )

    val updateRows = testRows.map { row =>
      row.copy(values = row.values.updated(0, row.values.head.mapString(s => s"${s}-updated")))
    }
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId).toList, updateRows)
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)._2,
      r => r should contain theSameElementsAs updateRows
    )

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber).take(1))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)._2,
      r => r should have size testRows.size.toLong - 1
    )

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, minRow, maxRow + 1)._2,
      r => r shouldBe empty
    )
  }

  it should "be possible to insert, update and delete sequence rows using externalId" in withSequence { sequence =>
    val externalId = sequence.externalId.get
    client.sequenceRows.insertByExternalId(externalId, sequence.columns.map(_.externalId).toList, testRows)
    val rows = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryByExternalId(externalId, minRow, maxRow + 1)._2,
      r => r should contain theSameElementsAs testRows
    )

    // note: intentionally using queryById here.
    val (_, rowsById) = client.sequenceRows.queryById(
      sequence.id, minRow, maxRow + 1)
    rowsById should contain theSameElementsAs testRows

    client.sequenceRows.deleteByExternalId(externalId, rows.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryByExternalId(externalId, minRow, maxRow + 1)._2,
      r => r shouldBe empty
    )
  }
}
