package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.{ReadBehaviours, RetryWhile, SdkTestSpec, SetNull, SetValue, WritableBehaviors}

class SequencesTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("sequence-5PNii0w", "sequence-6VhKQqt")
  val eid = shortRandom()

  (it should behave).like(readable(client.sequences))

  (it should behave).like(
    readableWithRetrieve(client.sequences, idsThatDoNotExist, supportsMissingAndThrown = true)
  )

  (it should behave).like(
    readableWithRetrieveByExternalId(
      client.sequences,
      externalIdsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  (it should behave).like(
    writable(
      client.sequences,
      Some(client.sequences),
      Seq(
        Sequence(
          name = Some("scala-sdk-write-example-1"),
          columns = NonEmptyList.of(
            SequenceColumn(name = Some("col1"), externalId = s"ext1-$eid", valueType = "DOUBLE"),
            SequenceColumn(name = Some("col2"), externalId = s"ext2-$eid")
          )
        ),
        Sequence(
          name = Some("scala-sdk-write-example-2"),
          columns = NonEmptyList.of(SequenceColumn(name = Some("col1"), externalId = s"ext1-$eid", valueType = "LONG"))
        )
      ),
      Seq(
        SequenceCreate(
          name = Some("scala-sdk-create-example-1"),
          columns = NonEmptyList.of(
            SequenceColumnCreate(name = Some("string-column"), externalId = s"ext2-$eid")
          )
        ),
        SequenceCreate(
          name = Some("scala-sdk-create-example-2"),
          columns = NonEmptyList.of(
            SequenceColumnCreate(name = Some("string-column"), externalId = s"string1-$eid"),
            SequenceColumnCreate(
              name = Some("long-column"),
              externalId = s"long1-$eid",
              valueType = "LONG"
            )
          )
        )
      ),
      idsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  (it should behave).like(
    writableWithExternalId(
      client.sequences,
      Some(client.sequences),
      Seq(
        Sequence(
          name = Some("scala-sdk-write-external-example-1"),
          externalId = Some(shortRandom()),
          columns =
            NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"ext2-$eid"))
        ),
        Sequence(
          name = Some("scala-sdk-write-external-example-2"),
          externalId = Some(shortRandom()),
          columns =
            NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"ext2-$eid"))
        )
      ),
      Seq(
        SequenceCreate(
          name = Some("scala-sdk-create-external-example-1"),
          externalId = Some(shortRandom()),
          columns = NonEmptyList.of(SequenceColumnCreate(name = Some("string-column"), externalId = s"string-column-$eid"))
        ),
        SequenceCreate(
          name = Some("scala-sdk-create-external-example-2"),
          externalId = Some(shortRandom()),
          columns = NonEmptyList.of(SequenceColumnCreate(name = Some("string-column"), externalId = s"string-column-$eid"))
        )
      ),
      externalIdsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  private val sequencesToCreate = Seq(
    Sequence(
      name = Some("scala-sdk-write-example-1"),
      description = Some("description-1"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid"))
    ),
    Sequence(
      name = Some("scala-sdk-write-example-2"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid"))
    ),
    Sequence(
      name = Some("scala-sdk-write-example-3"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid")),
      dataSetId = Some(testDataSet.id)
    )
  )
  private val sequencesUpdates = Seq(
    Sequence(
      name = Some("scala-sdk-write-example-1-1"),
      description = Some(null), // scalastyle:ignore null
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid"))
    ),
    Sequence(
      name = Some("scala-sdk-write-example-2-1"),
      description = Some("scala-sdk-write-example-2"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid")),
      dataSetId = Some(testDataSet.id)
    ),
    Sequence(
      name = Some("scala-sdk-write-example-3-1"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid")),
      dataSetId = Some(testDataSet.id)
    )
  )
  (it should behave).like(
    updatable(
      client.sequences,
      Some(client.sequences),
      sequencesToCreate,
      sequencesUpdates,
      (id: Long, item: Sequence) => item.copy(id = id),
      (a: Sequence, b: Sequence) => {
        a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) == b.copy(
          lastUpdatedTime = Instant.ofEpochMilli(0)
        )
      },
      (readSequence: Seq[Sequence], updatedSequence: Seq[Sequence]) => {
        assert(readSequence.size == sequencesUpdates.size)
        assert(readSequence.size == sequencesToCreate.size)
        assert(updatedSequence.size == sequencesUpdates.size)
        assert(updatedSequence.zip(readSequence).forall {
          case (updated, read) => updated.name == read.name.map(n => s"${n}-1")
        })
        assert(updatedSequence.head.description.isEmpty)
        assert(updatedSequence(1).description == sequencesUpdates(1).description)
        val dataSets = updatedSequence.map(_.dataSetId)
        assert(List(None, Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
        ()
      }
    )
  )

  it should behave like updatableById(
    client.sequences,
    Some(client.sequences),
    sequencesToCreate,
    Seq(
      SequenceUpdate(name = Some(SetValue("scala-sdk-write-example-1-1"))),
      SequenceUpdate(name = Some(SetValue("scala-sdk-write-example-2-1")), dataSetId = Some(SetValue(testDataSet.id))),
      SequenceUpdate(name = Some(SetValue("scala-sdk-write-example-3-1")), dataSetId = Some(SetNull()))
    ),
    (readSequences: Seq[Sequence], updatedSequences: Seq[Sequence]) => {
      assert(readSequences.size == updatedSequences.size)
      assert(updatedSequences.zip(readSequences).forall { case (updated, read) =>  updated.name.get == s"${read.name.get}-1" })
      val dataSets = updatedSequences.map(_.dataSetId)
      assert(List(None, Some(testDataSet.id), None) === dataSets)
      ()
    }
  )

  it should behave like updatableByExternalId(
    client.sequences,
    Some(client.sequences),
    Seq(Sequence(
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid")),
      externalId = Some(s"update-1-externalId-$eid")),
      Sequence(columns = NonEmptyList.of(
        SequenceColumn(name = Some("string-column"), externalId = s"string-column-$eid")), externalId = Some(s"update-2-externalId-$eid"))),
    Map(s"update-1-externalId-$eid" -> SequenceUpdate(externalId = Some(SetValue(s"update-1-externalId-$eid-1"))),
      s"update-2-externalId-$eid" -> SequenceUpdate(externalId = Some(SetValue(s"update-2-externalId-$eid-1")))),
    (readSequences: Seq[Sequence], updatedSequences: Seq[Sequence]) => {
      assert(readSequences.size == updatedSequences.size)
      assert(updatedSequences.zip(readSequences).forall { case (updated, read) =>
        updated.externalId.getOrElse("") == s"${read.externalId.getOrElse("")}-1" })
      ()
    }
  )

  it should "support search" in {
    val emptyCreatedTimeSearchResults = client.sequences
      .search(
        SequenceQuery(
          filter = Some(
            SequenceFilter(
              createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(0)))
            )
          )
        )
      )
    assert(emptyCreatedTimeSearchResults.isEmpty)
    val createdTimeSearchResults = client.sequences
      .search(
        SequenceQuery(
          filter = Some(
            SequenceFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1568975105000L)))
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 2)
    val createdTimeSearchResults2 = client.sequences.search(
      SequenceQuery(
        filter = Some(
          SequenceFilter(
            createdTime = Some(
              TimeRange(Instant.ofEpochMilli(1535964900000L), Instant.ofEpochMilli(1568979128000L))
            )
          )
        )
      )
    )
    assert(createdTimeSearchResults2.length == 5)

    val externalIdPrefixSearchResults = client.sequences.search(
      SequenceQuery(
        filter = Some(
          SequenceFilter(
            externalIdPrefix = Some("test"),
            createdTime =
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1568980123000L)))
          )
        )
      )
    )
    assert(externalIdPrefixSearchResults.length == 2)

    val nameSearchResults = client.sequences.search(
      SequenceQuery(
        filter = Some(
          SequenceFilter(
            externalIdPrefix = Some("test"),
            createdTime =
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1568980123000L)))
          )
        ),
        search = Some(SequenceSearch(name = Some("relevant")))
      )
    )
    assert(nameSearchResults.length == 1)

    val descriptionSearchResults = client.sequences.search(
      SequenceQuery(
        filter = Some(
          SequenceFilter(
            createdTime = Some(
              TimeRange(Instant.ofEpochMilli(0L), Instant.ofEpochMilli(1568979128000L))
            )
          )
        ),
        search = Some(SequenceSearch(description = Some("description")))
      )
    )
    assert(descriptionSearchResults.length == 1)

    val limitDescriptionSearchResults = client.sequences.search(
      SequenceQuery(
        limit = 1,
        filter = Some(
          SequenceFilter(
            externalIdPrefix = Some("test"),
            createdTime =
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1568980123000L)))
          )
        )
      )
    )
    assert(limitDescriptionSearchResults.length == 1)
  }

  it should "support search with dataSetIds" in {
    val created = client.sequences.createFromRead(sequencesToCreate)
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult(
        client.sequences.search(SequenceQuery(Some(SequenceFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = createdTimes.min,
            max = createdTimes.max
          ))
        )))),
        (a: Seq[_]) => a should not be empty
      )
      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain only (foundItems.map(_.id): _*)
    } finally {
      client.sequences.deleteByIds(created.map(_.id))
    }
  }
}
