// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{CdpApiException, ReadBehaviours, RetryWhile, SdkTestSpec, SetValue, WritableBehaviors}
import fs2.Stream
import org.scalatest.matchers.should.Matchers

import java.io.{BufferedInputStream, ByteArrayOutputStream, FileInputStream}
import java.nio.file.{Files, Paths}
import java.time.Instant
import java.util.UUID

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.ThreadSleep",
    "org.wartremover.warts.While",
    "org.wartremover.warts.IterableOps",
    "org.wartremover.warts.SizeIs"
  )
)
class FilesTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with Matchers with RetryWhile {
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readable(client.files)

  it should behave like partitionedReadable(client.files)

  it should behave like readableWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveByExternalId(client.files, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like readableWithRetrieveUnknownIds(client.dataSets)

  private val externalId = UUID.randomUUID().toString.substring(0, 8)

  private val filesToCreate = Seq(
    File(name = "scala-sdk-update-1", dataSetId = Some(testDataSet.id)),
    File(name = "scala-sdk-update-2", externalId = Some(s"${externalId}-2"))
  )
  private val filesUpdates = Seq(
    // `name` can not be changed, but is required here
    File(name = "scala-sdk-update-1", externalId = Some(s"${externalId}-1"), dataSetId = Some(testDataSet.id)),
    File(name = "scala-sdk-update-2", metadata = Some(Map("a" -> "b")), dataSetId = Some(testDataSet.id))
  )

  private def normalizeFile(f: File): File = {
    f.copy(
      // WORKAROUND: the API returns empty set and nothing kinda interchangeably
      metadata = Some(f.metadata.getOrElse(Map.empty)),
      lastUpdatedTime = Instant.ofEpochMilli(0)
    )
  }

  it should behave like updatable(
    client.files,
    Some(client.files),
    filesToCreate,
    filesUpdates,
    (id: Long, item: File) => item.copy(id = id),
    (a: File, b: File) => { normalizeFile(a) === normalizeFile(b) },
    (readFiles: Seq[File], updatedEvents: Seq[File]) => {
      assert(filesToCreate.size == filesUpdates.size)
      assert(readFiles.size == filesToCreate.size)
      assert(readFiles.size == updatedEvents.size)
      assert(updatedEvents.zip(readFiles).forall { case (updated, read) =>
          updated.name === read.name
      })
      assert(Some(s"${externalId}-1") === updatedEvents(0).externalId)
      assert("b" === updatedEvents(1).metadata.value.get("a").value)
      val dataSets = updatedEvents.map(_.dataSetId)
      assert(List(Some(testDataSet.id), Some(testDataSet.id)) === dataSets)
      ()
    }
  )


  it should "be an error to delete using ids that does not exist" in {
    val thrown = the[CdpApiException] thrownBy client.files
      .deleteByIds(idsThatDoNotExist).unsafeRunSync()
    val missingIds = thrown.missing
      .getOrElse(Seq.empty)
      .map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
    missingIds should have size idsThatDoNotExist.size.toLong
    missingIds should contain theSameElementsAs idsThatDoNotExist

    val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
    val sameIdsThrown = the[CdpApiException] thrownBy client.files
      .deleteByIds(sameIdsThatDoNotExist).unsafeRunSync()
    // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
    // if duplicated ids that do not exist are specified.
    val sameMissingIds = sameIdsThrown.duplicated match {
      case Some(duplicatedIds) =>
        duplicatedIds.map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
      case None =>
        sameIdsThrown.missing
          .getOrElse(Seq.empty)
          .map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
    }
    sameMissingIds should have size sameIdsThatDoNotExist.toSet.size.toLong
    sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
  }

  it should "create and delete items using the read class" in {
    // create a single item
    val testFile = File(name = "scala-sdk-read-example-1")
    val createdItem = client.files.createOneFromRead(testFile).unsafeRunSync()
    createdItem.name shouldBe testFile.name
    client.files.deleteByIds(Seq(createdItem.id)).unsafeRunSync()
    an[CdpApiException] should be thrownBy client.files.retrieveByIds(Seq(createdItem.id)).unsafeRunSync()
  }

  it should "create and delete items using the create class" in {
    // create a single item
    val testFile = FileCreate(name = "scala-sdk-read-example-1")
    val createdItem = client.files.createOne(testFile).unsafeRunSync()
    createdItem.name shouldBe testFile.name
    client.files.deleteByIds(Seq(createdItem.id)).unsafeRunSync()
    an[CdpApiException] should be thrownBy client.files.retrieveByIds(Seq(createdItem.id)).unsafeRunSync()
  }

  it should "allow updates by Id" in {
    val testFile = File(name = "test-file-1", externalId = Some("test-externalId-1"))
    val createdItem = client.files.createOneFromRead(testFile).unsafeRunSync()
    val updatedFile = client.files.updateById(Map(createdItem.id -> FileUpdate(externalId = Some(SetValue("test-externalId-1-1"))))).unsafeRunSync()
    assert(updatedFile.length == 1)
    assert(createdItem.name === updatedFile.head.name)
    assert(updatedFile.head.externalId.value === s"${testFile.externalId.value}-1")
    client.files.deleteByExternalId("test-externalId-1-1").unsafeRunSync()
  }

  it should "allow updates by externalId" in {
    val testFile = File(name = "test-file-1", externalId = Some("test-externalId-1"), source = Some("source-1"), directory = Some("/dir-1"))
    val createdItem = client.files.createOneFromRead(testFile).unsafeRunSync()
    val updatedFile = client.files.updateByExternalId(
      Map(
        createdItem.externalId.value -> FileUpdate(
          source = Some(SetValue("source-1-1")),
          directory = Some(SetValue("/dir-1-1"))
        )
      )
    ).unsafeRunSync()
    assert(updatedFile.length == 1)
    assert(createdItem.name === updatedFile.head.name)
    assert(updatedFile.head.source.value === s"${testFile.source.value}-1")
    assert(updatedFile.head.directory.value === s"${testFile.directory.value}-1")
    client.files.deleteByExternalId("test-externalId-1").unsafeRunSync()
  }

  it should "support filter" in {
    val createdTimeFilterResults = client.files
      .filter(
        FilesFilter(
          createdTime =
            Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
        )
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterResults.length == 22)

    val createdTimeFilterPartitionResults = client.files
      .filterPartitions(
        FilesFilter(
          createdTime =
            Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
        ), 10
      )
      .fold(Stream.empty)(_ ++ _)
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterPartitionResults.length == 22)

    val createdTimeFilterResultsWithLimit = client.files
      .filter(
        FilesFilter(
          createdTime =
            Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
        ),
        limit = Some(20)
      )
      .compile
      .toList
      .unsafeRunSync()
    assert(createdTimeFilterResultsWithLimit.length == 20)
  }

  it should "support search" in {
    val createdTimeSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
            )
          )
        )
      )
      .unsafeRunSync()
    assert(createdTimeSearchResults.length == 22)
    val mimeTypeTimeSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L)))),
              mimeType = Some("txt")
            )
          )
        )
      )
      .unsafeRunSync()
    assert(mimeTypeTimeSearchResults.length == 1)
    val nameSearchResults = client.files
      .search(
        FilesQuery(
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
            )
          ),
          search = Some(
            FilesSearch(
              name = Some("MyCadFile")
            )
          )
        )
      )
      .unsafeRunSync()
    assert(nameSearchResults.length == 3)
    val limitTimeSearchResults = client.files
      .search(
        FilesQuery(
          limit = 5,
          filter = Some(
            FilesFilter(
              createdTime =
                Some(TimeRange(Some(Instant.ofEpochMilli(0)), Some(Instant.ofEpochMilli(1563284224550L))))
            )
          )
        )
      )
      .unsafeRunSync()
    assert(limitTimeSearchResults.length == 5)
  }

  it should "support upload" in {
    val inputStream = new BufferedInputStream(
      new FileInputStream(
        new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt")
      )
    )
    val file =
      client.files.uploadWithName(
        inputStream,
        "uploadTest123.txt"
      ).unsafeRunSync()

    client.files.deleteById(file.id).unsafeRunSync()

    assert(file.name === "uploadTest123.txt")
  }

  it should "support download" in {
    val file =
      client.files.upload(
        new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt")
      ).unsafeRunSync()

    var uploadedFile = client.files.retrieveByIds(Seq(file.id)).unsafeRunSync()
    var retryCount = 0
    while (!uploadedFile.headOption
        .getOrElse(throw new RuntimeException("File was not uploaded in test"))
        .uploaded) {
      retryCount += 1
      Thread.sleep(500)
      uploadedFile = client.files.retrieveByIds(Seq(file.id)).unsafeRunSync()
      if (retryCount > 10) {
        throw new RuntimeException("File is not uploaded after 10 retries in test")
      }
    }

    val out = new ByteArrayOutputStream()
    client.files.download(FileDownloadId(file.id), out).unsafeRunSync()

    val expected =
      Files.readAllBytes(Paths.get("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt"))

    assert(out.toByteArray.sameElements(expected))
    client.files.deleteById(file.id).unsafeRunSync()
  }

  it should "support returning download link" in {
    val file =
      client.files.upload(
        new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt")
      ).unsafeRunSync()


    var uploadedFile = client.files.retrieveByIds(Seq(file.id)).unsafeRunSync()
    var retryCount = 0
    while (!uploadedFile.headOption
      .getOrElse(throw new RuntimeException("File was not uploaded in test"))
      .uploaded) {
      retryCount += 1
      Thread.sleep(500)
      uploadedFile = client.files.retrieveByIds(Seq(file.id)).unsafeRunSync()
      if (retryCount > 10) {
        throw new RuntimeException("File is not uploaded after 10 retries in test")
      }
    }

    val link = client.files.downloadLink(FileDownloadId(file.id)).unsafeRunSync()


    link.downloadUrl shouldNot be(empty)
    client.files.deleteById(file.id).unsafeRunSync()
  }

  it should "support search with dataSetIds" in {
    val created = filesToCreate.map(f => client.files.createOneFromRead(f).unsafeRunSync())
    try {
      val createdTimes = created.map(_.createdTime)
      val foundItems = retryWithExpectedResult[Seq[File]](
        client.files.search(FilesQuery(Some(FilesFilter(
          dataSetIds = Some(Seq(CogniteInternalId(testDataSet.id))),
          createdTime = Some(TimeRange(
            min = Some(createdTimes.min),
            max = Some(createdTimes.max)
          ))
        )))).unsafeRunSync(),
        a => a should not be empty
      )

      foundItems.map(_.dataSetId) should contain only Some(testDataSet.id)
      created.filter(_.dataSetId.isDefined).map(_.id) should contain theSameElementsAs foundItems.map(_.id)
    } finally {
      client.files.deleteByIds(created.map(_.id)).unsafeRunSync()
    }
  }
}
