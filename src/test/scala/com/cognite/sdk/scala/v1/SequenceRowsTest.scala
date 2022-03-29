// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common._
import io.circe.syntax._
import org.scalatest.{OptionValues, ParallelTestExecution}

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.NonUnitStatements"))
class SequenceRowsTest extends SdkTestSpec with ParallelTestExecution with RetryWhile with OptionValues {
  def withSequence(testCode: Sequence => Any): Unit = {
    val externalId = shortRandom()
    val sequence = client.sequences.createOneFromRead(
      Sequence(
        name = Some(s"sequence-rows-test-$externalId"),
        externalId = Some(externalId),
        columns =
          NonEmptyList.of(
            SequenceColumn(name = Some("string-column"), externalId = Some(s"ext1-$externalId")),
            SequenceColumn(name = Some("long-column"), externalId = Some(s"ext2-$externalId"), valueType = "LONG")
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
    SequenceRow(1, Seq("string-1".asJson, 1L.asJson)),
    SequenceRow(2, Seq("string-2".asJson, (-1L).asJson)),
    SequenceRow(3, Seq("string-3".asJson, 100L.asJson)),
    SequenceRow(4, Seq("string-4".asJson, 1000L.asJson)),
    SequenceRow(5, Seq("string-5".asJson, 10000L.asJson)),
    SequenceRow(6, Seq("string-6".asJson, 100000L.asJson))
  )
  private val minRow = testRows.map(_.rowNumber).min
  private val maxRow = testRows.map(_.rowNumber).max

  it should "be possible to insert, update, and delete sequence rows using internalId" in withSequence { sequence =>
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId.getOrElse(
      throw new RuntimeException("Unexpected missing column externalId"))).toList, testRows)
    val rows = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r should contain theSameElementsAs testRows
    )
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, Some(minRow), Some(maxRow + 1), batchSize = 1)._2.compile.toList,
      r => r should contain theSameElementsAs testRows
    )

    val updateRows = testRows.map { row =>
      row.copy(values = row.values.updated(0, row.values.head.mapString(s => s"${s}-updated")))
    }
    client.sequenceRows.insertById(sequence.id, sequence.columns.map(_.externalId.getOrElse(
      throw new RuntimeException("Unexpected missing column externalId"))).toList, updateRows)
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r should contain theSameElementsAs updateRows
    )

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber).take(1))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r should have size testRows.size.toLong - 1
    )

    client.sequenceRows.deleteById(sequence.id, rows.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryById(sequence.id, Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r shouldBe empty
    )
  }

  it should "be possible to insert, update and delete sequence rows using externalId" in withSequence { sequence =>
    val externalId = sequence.externalId.value
    client.sequenceRows.insertByExternalId(externalId, sequence.columns.map(_.externalId.getOrElse(
      throw new RuntimeException("Unexpected missing column externalId"))).toList, testRows)
    val rows = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryByExternalId(externalId, None, None)._2.compile.toList,
      r => r should contain theSameElementsAs testRows
    )

    // note: intentionally using queryById here.
    val (_, rowsByIdStream) = client.sequenceRows.queryById(
      sequence.id, Some(minRow), Some(maxRow + 1))
    val rowsById: Seq[SequenceRow] = rowsByIdStream.compile.toList
    rowsById should contain theSameElementsAs testRows

    client.sequenceRows.deleteByExternalId(externalId, rows.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.queryByExternalId(externalId, Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r shouldBe empty
    )
  }

  it should "be possible to insert and delete sequence rows using cogniteId" in withSequence { sequence =>
    val externalId = sequence.externalId.value
    client.sequenceRows.insert(CogniteExternalId(externalId), sequence.columns.map(_.externalId.getOrElse(
      throw new RuntimeException("Unexpected missing column externalId"))).toList, testRows)
    val rows = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.query(CogniteExternalId(externalId), None, None)._2.compile.toList,
      r => r should contain theSameElementsAs testRows
    )

    client.sequenceRows.delete(CogniteExternalId(externalId), rows.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.query(CogniteExternalId(externalId), Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r shouldBe empty
    )

    val internalId = sequence.id
    client.sequenceRows.insert(CogniteInternalId(internalId), sequence.columns.map(_.externalId.getOrElse(
      throw new RuntimeException("Unexpected missing column externalId"))).toList, testRows)
    val rows2 = retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.query(CogniteInternalId(internalId), None, None)._2.compile.toList,
      r => r should contain theSameElementsAs testRows
    )

    client.sequenceRows.delete(CogniteInternalId(internalId), rows2.map(_.rowNumber))
    retryWithExpectedResult[Seq[SequenceRow]](
      client.sequenceRows.query(CogniteInternalId(internalId), Some(minRow), Some(maxRow + 1))._2.compile.toList,
      r => r shouldBe empty
    )
  }
}
