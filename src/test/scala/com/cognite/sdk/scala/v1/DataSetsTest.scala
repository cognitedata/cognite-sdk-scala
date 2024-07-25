// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1
import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.common.SetValue

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Null",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.SizeIs"
  )
)
class DataSetsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")
  (it should behave).like(readable(client.dataSets))

  (it should behave).like(
    readableWithRetrieve(client.dataSets, idsThatDoNotExist, supportsMissingAndThrown = true)
  )

  (it should behave).like(
    readableWithRetrieveByExternalId(
      client.dataSets,
      externalIdsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  (it should behave).like(readableWithRetrieveUnknownIds(client.dataSets))

  (it should behave).like(
    writable(
      client.dataSets,
      None,
      Seq(
        DataSet(name = Some("scala-sdk-read-example-1")),
        DataSet(name = Some("scala-sdk-read-example-2"))
      ),
      Seq(
        DataSetCreate(name = Some("scala-sdk-create-example-1")),
        DataSetCreate(name = Some("scala-sdk-create-example-2"))
      ),
      idsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  (it should behave).like(
    writableWithExternalId(
      client.dataSets,
      None,
      Seq(
        DataSet(name = Some("scala-sdk-read-example-1"), externalId = Some(shortRandom())),
        DataSet(name = Some("scala-sdk-read-example-2"), externalId = Some(shortRandom())),
        DataSet(name = Some("scala-sdk-read-example-3"), externalId = Some(shortRandom()))
      ),
      Seq(
        DataSetCreate(name = Some("scala-sdk-create-example-1"), externalId = Some(shortRandom())),
        DataSetCreate(name = Some("scala-sdk-create-example-2"), externalId = Some(shortRandom())),
        DataSetCreate(name = Some("scala-sdk-create-example-3"), externalId = Some(shortRandom()))
      ),
      externalIdsThatDoNotExist,
      supportsMissingAndThrown = true
    )
  )

  private val datasetsToCreate = Seq(
    DataSet(description = Some("desc-1"), name = Some("scala-sdk-update-1")),
    DataSet(description = Some("desc-2")),
    DataSet(description = Some("desc-3"))
  )

  private val datasetUpdates = Seq(
    DataSet(description = Some("desc-1-1"), name = null),
    DataSet(
      description = Some("desc-2-1"),
      metadata = Some(Map("a" -> "b"))
    ),
    DataSet(description = Some("desc-3-1"))
  )

  (it should behave).like(
    updatable(
      client.dataSets,
      None,
      datasetsToCreate,
      datasetUpdates,
      (id: Long, item: DataSet) => item.copy(id = id),
      (a: DataSet, b: DataSet) =>
        a.copy(lastUpdatedTime = Instant.ofEpochMilli(0)) === b.copy(lastUpdatedTime =
          Instant.ofEpochMilli(0)
        ),
      (readDatasets: Seq[DataSet], updatedDatasets: Seq[DataSet]) => {
        assert(datasetsToCreate.size == datasetUpdates.size)
        assert(readDatasets.size == datasetsToCreate.size)
        assert(readDatasets.size == updatedDatasets.size)
        assert(updatedDatasets.zip(readDatasets).forall { case (updated, read) =>
          updated.description.nonEmpty &&
          read.description.nonEmpty &&
          updated.description.forall { description =>
            description === s"${read.description.value}-1"
          }
        })
        assert(readDatasets.head.name.isDefined)
        assert(updatedDatasets.head.name.isEmpty)
        assert(updatedDatasets(1).name === datasetUpdates(1).name)
        ()
      }
    )
  )

  (it should behave).like(
    updatableById(
      client.dataSets,
      None,
      datasetsToCreate,
      Seq(
        DataSetUpdate(description = Some(SetValue("desc-1-1")), name = Some(SetNull())),
        DataSetUpdate(description = Some(SetValue("desc-2-1"))),
        DataSetUpdate(description = Some(SetValue("desc-3-1")))
      ),
      (readDatasets: Seq[DataSet], updatedDatasets: Seq[DataSet]) => {
        assert(readDatasets.size == updatedDatasets.size)
        assert(updatedDatasets.zip(readDatasets).forall { case (updated, read) =>
          updated.description.value === s"${read.description.value}-1"
        })
        val names = updatedDatasets.map(_.name)
        assert(List(None, None, None) === names)
        ()
      }
    )
  )

  private val externalId = shortRandom()

  (it should behave).like(
    updatableByExternalId(
      client.dataSets,
      None,
      Seq(
        DataSet(
          description = Some("description-1"),
          externalId = Some(s"update-1-externalId-${externalId}")
        ),
        DataSet(
          description = Some("description-2"),
          externalId = Some(s"update-2-externalId-${externalId}")
        )
      ),
      Map(
        s"update-1-externalId-${externalId}" -> DataSetUpdate(description =
          Some(SetValue("description-1-1"))
        ),
        s"update-2-externalId-${externalId}" -> DataSetUpdate(description =
          Some(SetValue("description-2-1"))
        )
      ),
      (readDatasets: Seq[DataSet], updatedDatasets: Seq[DataSet]) => {
        assert(readDatasets.size == updatedDatasets.size)
        assert(updatedDatasets.zip(readDatasets).forall { case (updated, read) =>
          updated.description.getOrElse("") === s"${read.description.getOrElse("")}-1"
        })
        assert(updatedDatasets.zip(readDatasets).forall { case (updated, read) =>
          updated.externalId === read.externalId
        })
        ()
      }
    )
  )

  it should "support filter" in {
    val createdTimeFilterResults = client.dataSets
      .filter(
        DataSetFilter(
          createdTime = Some(
            TimeRange(
              Some(Instant.ofEpochMilli(1584403200000L)),
              Some(Instant.ofEpochMilli(1584525600000L))
            )
          )
        )
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterResults.length == 4)

    val createdTimeFilterResultsWithLimit = client.dataSets
      .filter(
        DataSetFilter(
          createdTime = Some(
            TimeRange(
              Some(Instant.ofEpochMilli(1584403200000L)),
              Some(Instant.ofEpochMilli(1584525600000L))
            )
          ),
          externalIdPrefix = Some("test")
        ),
        limit = Some(1)
      )
      .compile
      .toList
      .unsafeRunSync()

    assert(createdTimeFilterResultsWithLimit.length == 1)
  }
}
