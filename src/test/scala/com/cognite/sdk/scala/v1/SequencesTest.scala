package com.cognite.sdk.scala.v1

import java.time.Instant

import cats.data.NonEmptyList
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class SequencesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("sequence-5PNii0w", "sequence-6VhKQqt")

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
      Seq(
        Sequence(
          name = Some("scala-sdk-write-example-1"),
          columns = NonEmptyList.of(
            SequenceColumn(name = Some("col1"), externalId = "ext1", valueType = "DOUBLE"),
            SequenceColumn(name = Some("col2"), externalId = "ext2")
          )
        ),
        Sequence(
          name = Some("scala-sdk-write-example-2"),
          columns = NonEmptyList.of(SequenceColumn(name = Some("col1"), externalId = "ext1", valueType = "LONG"))
        )
      ),
      Seq(
        SequenceCreate(
          name = Some("scala-sdk-create-example-1"),
          columns = NonEmptyList.of(
            SequenceColumnCreate(name = Some("string-column"), externalId = "ext2")
          )
        ),
        SequenceCreate(
          name = Some("scala-sdk-create-example-2"),
          columns = NonEmptyList.of(
            SequenceColumnCreate(name = Some("string-column"), externalId = "string1"),
            SequenceColumnCreate(
              name = Some("long-column"),
              externalId = "long1",
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
      Seq(
        Sequence(
          name = Some("scala-sdk-write-external-example-1"),
          externalId = Some(shortRandom()),
          columns =
            NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "ext2"))
        ),
        Sequence(
          name = Some("scala-sdk-write-external-example-2"),
          externalId = Some(shortRandom()),
          columns =
            NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "ext2"))
        )
      ),
      Seq(
        SequenceCreate(
          name = Some("scala-sdk-create-external-example-1"),
          externalId = Some(shortRandom()),
          columns = NonEmptyList.of(SequenceColumnCreate(name = Some("string-column"), externalId = "string-column"))
        ),
        SequenceCreate(
          name = Some("scala-sdk-create-external-example-2"),
          externalId = Some(shortRandom()),
          columns = NonEmptyList.of(SequenceColumnCreate(name = Some("string-column"), externalId = "string-column"))
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
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "string-column"))
    ),
    Sequence(
      name = Some("scala-sdk-write-example-2"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "string-column"))
    )
  )
  private val sequencesUpdates = Seq(
    Sequence(
      name = Some("scala-sdk-write-example-1-1"),
      description = Some(null), // scalastyle:ignore null
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "string-column"))
    ),
    Sequence(
      name = Some("scala-sdk-write-example-2-1"),
      description = Some("scala-sdk-write-example-2"),
      columns = NonEmptyList.of(SequenceColumn(name = Some("string-column"), externalId = "string-column"))
    )
  )
  (it should behave).like(
    updatable(
      client.sequences,
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
        ()
      }
    )
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
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1566911825370L)))
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
              TimeRange(Instant.ofEpochMilli(1535964900000L), Instant.ofEpochMilli(1566979600295L))
            )
          )
        )
      )
    )
    assert(createdTimeSearchResults2.length == 8)

    val externalIdPrefixSearchResults = client.sequences.search(
      SequenceQuery(
        filter = Some(
          SequenceFilter(
            externalIdPrefix = Some("test"),
            createdTime =
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1566979600295L)))
          )
        )
      )
    )
    assert(externalIdPrefixSearchResults.length == 2)

    val nameSearchResults = client.sequences
      .search(
        SequenceQuery(
          filter = Some(
            SequenceFilter(
              externalIdPrefix = Some("test"),
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1566979600295L)))
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
              TimeRange(Instant.ofEpochMilli(0L), Instant.ofEpochMilli(1566979600295L))
            )
          )
        ),
        search = Some(SequenceSearch(description = Some("Optional")))
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
              Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1566979600295L)))
          )
        )
      )
    )
    assert(limitDescriptionSearchResults.length == 1)
  }
}
