package com.cognite.sdk.scala.v1

import java.time.Instant
import java.util.UUID

import org.scalatest.Matchers

import com.cognite.sdk.scala.common.{CdpApiException, ReadBehaviours, SdkTest, WritableBehaviors}

class FilesTest extends SdkTest with ReadBehaviours with WritableBehaviors with Matchers {
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.files)

  it should behave like readableWithRetrieve(
    client.files,
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  private val externalId = UUID.randomUUID().toString.substring(0, 8)

  it should "be an error to delete using ids that does not exist" in {
    val thrown = the[CdpApiException] thrownBy client.files
      .deleteByIds(idsThatDoNotExist)
    val missingIds = thrown.missing
      .getOrElse(Seq.empty)
      .flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
    missingIds should have size idsThatDoNotExist.size.toLong
    missingIds should contain theSameElementsAs idsThatDoNotExist

    val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
    val sameIdsThrown = the[CdpApiException] thrownBy client.files
      .deleteByIds(sameIdsThatDoNotExist)
    // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
    // if duplicated ids that do not exist are specified.
    val sameMissingIds = sameIdsThrown.duplicated match {
      case Some(duplicatedIds) =>
        duplicatedIds.flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
      case None =>
        sameIdsThrown.missing
          .getOrElse(Seq.empty)
          .flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
    }
    sameMissingIds should have size sameIdsThatDoNotExist.toSet.size.toLong
    sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
  }

  it should "create and delete items using the read class" in {
    // create a single item
    val testFile = File(name = "scala-sdk-read-example-1")
    val createdItem = client.files.createOneFromRead(testFile)
    createdItem.name shouldBe testFile.name
    client.files.deleteByIds(Seq(createdItem.id))
    an[CdpApiException] should be thrownBy client.files.retrieveByIds(Seq(createdItem.id))
  }

  it should "create and delete items using the create class" in {
    // create a single item
    val testFile = FileCreate(name = "scala-sdk-read-example-1")
    val createdItem = client.files.createOne(testFile)
    createdItem.name shouldBe testFile.name
    client.files.deleteByIds(Seq(createdItem.id))
    an[CdpApiException] should be thrownBy client.files.retrieveByIds(Seq(createdItem.id))
  }

  it should "allow updates using the read class" in {
    // create items
    val testFile = File(name = "scala-sdk-read-example-1")
    val createdItem = client.files.createOneFromRead(testFile)
    assert(createdItem.name == testFile.name)
    createdItem.id should not be 0

    val readItems = client.files.retrieveByIds(Seq(createdItem.id))

    // update the item with new values
    val updateFile =
      File(
        name = "scala-sdk-update-1-1",
        id = createdItem.id,
        externalId = Some(s"${externalId}-1")
      )
    val updatedItems = client.files.updateFromRead(Seq(updateFile))
    assert(updatedItems.size == readItems.size)
    assert(updatedItems.forall {
      case (updated) => updated.externalId == updateFile.externalId
    })

    // delete it
    client.files.deleteByIds(Seq(createdItem.id))
  }

  it should "support filter" in {
    val createdTimeFilterResults = client.files
      .filter(
        FilesFilter(
          createdTime =
            Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))
        )
      )
      .compile
      .toList
    assert(createdTimeFilterResults.length == 25)

    val createdTimeFilterResultsWithLimit = client.files
      .filterWithLimit(
        FilesFilter(
          createdTime =
            Some(TimeRange(Instant.ofEpochMilli(0), Instant.ofEpochMilli(1563284224550L)))
        ),
        20
      )
      .compile
      .toList
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
    assert(createdTimeSearchResults.length == 25)
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
  it should "support upload" in {
    val file =
      client.files.uploadWithName(
        new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt"),
        "uploadTest123.txt"
      )

    client.files.deleteById(file.id)

    assert(file.name == "uploadTest123.txt")
  }
}
