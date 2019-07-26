package com.cognite.sdk.scala.v1

import java.time.Instant
import java.util.UUID

import cats.{Functor, Id}
import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class FilesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(implicitly[Functor[Id]], auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.files)

  it should behave like readableWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writable(
    client.files,
    Seq(File(name = "scala-sdk-read-example-1")),
    Seq(FileCreate(name = "scala-sdk-read-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val externalId = UUID.randomUUID().toString.substring(0, 8)
  private val filesToCreate = Seq(
    File(
      name = "scala-sdk-update-1",
      source = Some("scala-sdk-update-1"),
      externalId = Some(externalId),
      metadata = Some(Map()),
      assetIds = Some(Seq[Long]())
    )
  )
  private val fileUpdates = Seq(
    File(name = "scala-sdk-update-1-1", source = Some(null), externalId = Some(s"${externalId}-1")) // scalastyle:ignore null
  )
  it should behave like updatable(
    client.files,
    filesToCreate,
    fileUpdates,
    (id: Long, item: File) => item.copy(id = id),
    (a: File, b: File) => { a.copy(lastUpdatedTime = b.lastUpdatedTime) == b },
    (readFiles: Seq[File], updatedFiles: Seq[File]) => {
      assert(filesToCreate.size == fileUpdates.size)
      assert(readFiles.size == filesToCreate.size)
      assert(readFiles.size == updatedFiles.size)
      assert(updatedFiles.zip(readFiles).forall { case (updated, read) =>
        updated.externalId == read.externalId.map(id => s"${id}-1")
      })
      assert(readFiles.head.source.isDefined)
      assert(updatedFiles.head.source.isEmpty)
      ()
    }
  )

  it should "support filter" in {
    val createdTimeFilterResults = client.files
      .filter(
        FilesFilter(
          createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L))))
      ).flatMap(_.toList)
    assert(createdTimeFilterResults.length == 29)

    val createdTimeFilterResultsWithLimit = client.files
      .filterWithLimit(
        FilesFilter(
          createdTime = Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))),
        20
      ).flatMap(_.toList)
    assert(createdTimeFilterResultsWithLimit.length == 20)
  }

  it should "support search" in {
    val createdTimeSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))
            )
          )
        )
      )
    assert(createdTimeSearchResults.length == 29)
    val mimeTypeTimeSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L))),
              mimeType = Some("txt")
            )
          )
        )
      )
    assert(mimeTypeTimeSearchResults.length == 1)
    val nameSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))
            )
          ),
          search = Some(
            FilesSearch(
              name = Some("MyCadFile")
            )
          )
        )
      )
    assert(nameSearchResults.length == 4)
    val limitTimeSearchResults = client.files
      .search(
        FilesQuery(
          limit = 5,
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))
            )
          )
        )
      )
    assert(limitTimeSearchResults.length == 5)
  }
}
