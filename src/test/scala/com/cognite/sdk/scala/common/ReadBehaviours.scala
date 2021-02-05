// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import com.softwaremill.sttp.Id
import org.scalatest.{FlatSpec, Matchers}

trait ReadBehaviours extends Matchers { this: FlatSpec =>
  def readable[R, InternalId, PrimitiveId](
      readable: Readable[R, Id],
      supportsLimit: Boolean = true
  ): Unit = {
    val listLength = readable.list(Some(100)).compile.toList.length
    it should "read items" in {
      readable.read().items should not be empty
    }

    if (supportsLimit) {
      it should "read items with limit" in {
        (readable.read(limit = Some(1)).items should have).length(Math.min(listLength.toLong, 1))
        (readable.read(limit = Some(2)).items should have).length(Math.min(listLength.toLong, 2))
      }
    }

    it should "read all items" in {
      val first1Length = readable.list().take(1).compile.toList.length
      first1Length should be(Math.min(listLength, 1))
      if (supportsLimit) {
        val first2Length = readable.list(limit = Some(2)).compile.toList.length
        first2Length should be(Math.min(listLength, 2))
        val allLength = readable.list(limit = Some(3)).compile.toList.length
        allLength should be(Math.min(listLength, 3))
      }
    }
  }

  def partitionedReadable[R, InternalId, PrimitiveId](
      readable: PartitionedReadable[R, Id]
  ): Unit = {
    it should "read items with partitions" in {
      val partitionStreams = readable.listPartitions(2)
      val partitionStreams10 = readable.listPartitions(10)
      (partitionStreams should have).length(2)
      (partitionStreams10 should have).length(10)
      val partition1Items = partitionStreams.head.take(2).compile.toList
      val partition2Items = partitionStreams(1).take(2).compile.toList
      partition1Items.size should be <= 2
      partition2Items.size should be <= 2
      partition1Items.headOption should not be (partition2Items.headOption)
    }

    it should "read item partitions with limit" in {
      val partitionStreams = readable.listPartitions(2, limitPerPartition = Some(2))
      (partitionStreams should have).length(2)
      val partition1Items = partitionStreams.head.compile.toList
      val partition2Items = partitionStreams(1).compile.toList
      partition1Items.size should be <= 2
      partition2Items.size should be <= 2
    }

    it should "read all items using partitions" in {
      val first1Length = readable.list().take(1).compile.toList.length
      first1Length should be(1)
      val first2Length = readable.list(Some(2)).compile.toList.length
      first2Length should be(2)
      val allLength = readable.list(Some(3)).compile.toList.length
      allLength should be(3)
      val unlimitedLength = readable.list().map(_ => 1).compile.toList.length
      val partitionsLength = readable
        .listPartitions(40)
        .reduce(_ ++ _)
        .map(_ => 1)
        .compile
        .toList
        .length
      assert(unlimitedLength <= partitionsLength)
    }
  }

  // scalastyle:off
  def readableWithRetrieve[R <: WithId[Long], W](
      readable: Readable[R, Id] with RetrieveByIds[R, Id],
      idsThatDoNotExist: Seq[Long],
      supportsMissingAndThrown: Boolean
  ): Unit = {
    it should "support retrieving items by id" in {
      val firstTwoItemIds = readable.read(limit = Some(2)).items.map(_.id)
      firstTwoItemIds should have size 2
      val maybeItemsRead = readable.retrieveByIds(firstTwoItemIds)
      val itemsReadIds = maybeItemsRead.map(_.id)
      itemsReadIds should have size firstTwoItemIds.size.toLong
      itemsReadIds should contain theSameElementsAs firstTwoItemIds
    }

    it should "return information about missing ids" in {
      val thrown = the[CdpApiException] thrownBy readable
        .retrieveByIds(idsThatDoNotExist)
      if (supportsMissingAndThrown) {
        val itemsNotFound = thrown.missing

        val notFoundIds =
          itemsNotFound.get.flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
        notFoundIds should have size idsThatDoNotExist.size.toLong
        notFoundIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy readable
        .retrieveByIds(sameIdsThatDoNotExist)
      if (supportsMissingAndThrown) {
        sameIdsThrown.missing match {
          case Some(missingItems) =>
            val sameNotFoundIds =
              missingItems.flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong).toSet
            // it's a bit funny that the same missing ids are returned duplicated,
            // but that's how it works as of 2019-06-02.
            //sameNotFoundIds should have size sameIdsThatDoNotExist.size.toLong
            sameNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          case None =>
            val duplicatedNotFoundIds = sameIdsThrown.duplicated.get
              .flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
              .toSet
            //duplicatedNotFoundIds should have size sameIdsThatDoNotExist.toSet.size.toLong
            duplicatedNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
        }
      }
    }
  }

  private def fetchTestItemsWithRequiredExternalIds[R <: WithRequiredExternalId with WithCreatedTime](readable: Readable[R, Id]): List[R] = {
    // only use rows older than 10 minutes, to exclude items created by concurrently running tests which might be deleted quickly
    val minAge = Instant.now().minus(10, ChronoUnit.MINUTES)
    readable
      .list()
      .filter(_.createdTime.isBefore(minAge))
      .take(2)
      .compile
      .toList
  }

  def readableWithRetrieveByRequiredExternalId[R <: WithRequiredExternalId with WithCreatedTime, W](
     readable: Readable[R, Id] with RetrieveByExternalIds[R, Id],
     idsThatDoNotExist: Seq[String],
     supportsMissingAndThrown: Boolean
   ): Unit = {
    it should "support retrieving items by external id" in {
      // TODO: this test is not very stable as the fetched item may be deleted before it is fetched again by the external id
      val firstTwoItemIds = fetchTestItemsWithRequiredExternalIds(readable).map(_.externalId)

      firstTwoItemIds should have size 2
      val maybeItemsRead = readable.retrieveByExternalIds(firstTwoItemIds)
      val itemsReadIds = maybeItemsRead.map(_.externalId)
      itemsReadIds should have size firstTwoItemIds.size.toLong
      itemsReadIds should contain theSameElementsAs firstTwoItemIds
    }

    it should "return information about missing external ids" in {
      val thrown = the[CdpApiException] thrownBy readable
        .retrieveByExternalIds(idsThatDoNotExist)
      if (supportsMissingAndThrown) {
        val itemsNotFound = thrown.missing

        val notFoundIds =
          itemsNotFound.get.map(jsonObj => jsonObj("externalId").get.asString.get)
        notFoundIds should have size idsThatDoNotExist.size.toLong
        notFoundIds should contain theSameElementsAs idsThatDoNotExist
      }
    }
  }


  private def fetchTestItems[R <: WithExternalId with WithCreatedTime](readable: Readable[R, Id]): List[R] = {
    // only use rows older than 10 minutes, to exclude items created by concurrently running tests which might be deleted quickly
    val minAge = Instant.now().minus(10, ChronoUnit.MINUTES)
    readable
      .list()
      .filter(_.externalId.isDefined)
      .filter(_.createdTime.isBefore(minAge))
      .take(2)
      .compile
      .toList
  }

  def readableWithRetrieveByExternalId[R <: WithExternalId with WithCreatedTime, W](
      readable: Readable[R, Id] with RetrieveByExternalIds[R, Id],
      idsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean
  ): Unit = {
    it should "support retrieving items by external id" in {
      // TODO: this test is not very stable as the fetched item may be deleted before it is fetched again by the external id
      val firstTwoItemIds = fetchTestItems(readable).map(_.externalId.get)

      firstTwoItemIds should have size 2
      val maybeItemsRead = readable.retrieveByExternalIds(firstTwoItemIds)
      val itemsReadIds = maybeItemsRead.map(_.externalId.get)
      itemsReadIds should have size firstTwoItemIds.size.toLong
      itemsReadIds should contain theSameElementsAs firstTwoItemIds
    }

    it should "return information about missing external ids" in {
      val thrown = the[CdpApiException] thrownBy readable
        .retrieveByExternalIds(idsThatDoNotExist)
      if (supportsMissingAndThrown) {
        val itemsNotFound = thrown.missing

        val notFoundIds =
          itemsNotFound.get.map(jsonObj => jsonObj("externalId").get.asString.get)
        notFoundIds should have size idsThatDoNotExist.size.toLong
        notFoundIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy readable
        .retrieveByExternalIds(sameIdsThatDoNotExist)
      if (supportsMissingAndThrown) {
        sameIdsThrown.missing match {
          case Some(missingItems) =>
            val sameNotFoundIds =
              missingItems.map(jsonObj => jsonObj("externalId").get.asString.get).toSet
            // it's a bit funny that the same missing ids are returned duplicated,
            // but that's how it works as of 2019-06-02.
            //sameNotFoundIds should have size sameIdsThatDoNotExist.size.toLong
            sameNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          case None =>
            val duplicatedNotFoundIds = sameIdsThrown.duplicated.get
              .map(jsonObj => jsonObj("externalId").get.asString.get)
              .toSet
            //duplicatedNotFoundIds should have size sameIdsThatDoNotExist.toSet.size.toLong
            duplicatedNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
        }
      }
    }
  }

  def readableWithRetrieveUnknownIds[R <: WithExternalId with WithId[Long] with WithCreatedTime, W](
      readable: Readable[R, Id]
        with RetrieveByExternalIdsWithIgnoreUnknownIds[R, Id]
        with RetrieveByIdsWithIgnoreUnknownIds[R, Id]
  ): Unit = {
    val firstTwoItemItems = fetchTestItems(readable)
    val firstTwoExternalIds = firstTwoItemItems.map(_.externalId.get)
    val firstTwoIds = firstTwoItemItems.map(_.id)
    val nonExistentExternalId = "does-not-exist/" + UUID.randomUUID
    val nonExistentId = ThreadLocalRandom.current().nextLong(1, 9007199254740991L)

    it should "support retrieving items by external id with ignoreUnknownIds=true" in {
      firstTwoExternalIds should have size 2
      val maybeItemsRead = readable.retrieveByExternalIds(
        firstTwoExternalIds ++ Seq(nonExistentExternalId),
        ignoreUnknownIds = true
      )
      val itemsReadIds = maybeItemsRead.map(_.externalId.get)
      itemsReadIds should contain theSameElementsAs firstTwoExternalIds
      itemsReadIds should have size firstTwoExternalIds.size.toLong
    }

    it should "allow retrieving empty items with ignoreUnknownIds=true" in {
      val maybeItemsRead =
        readable.retrieveByExternalIds(Seq(nonExistentExternalId), ignoreUnknownIds = true)
      maybeItemsRead shouldBe empty
    }

    it should "throw when retrieving items by external id with ignoreUnknownIds=false" in {
      val exception = intercept[CdpApiException] {
        readable.retrieveByExternalIds(
          firstTwoExternalIds ++ Seq(nonExistentExternalId),
          ignoreUnknownIds = false
        )
      }
      exception.message should (include("ids not found") or include("id not found"))
    }

    it should "support retrieving items by id with ignoreUnknownIds=true" in {
      val maybeItemsRead =
        readable.retrieveByIds(firstTwoIds ++ Seq(nonExistentId), ignoreUnknownIds = true)
      val itemsReadIds = maybeItemsRead.map(_.externalId.get)
      itemsReadIds should contain theSameElementsAs firstTwoExternalIds
      itemsReadIds should have size firstTwoExternalIds.size.toLong
    }

    it should "throw when retrieving items by id with ignoreUnknownIds=false" in {
      val exception = intercept[CdpApiException] {
        readable.retrieveByIds(firstTwoIds ++ Seq(nonExistentId), ignoreUnknownIds = false)
      }
      exception.message should (include("ids not found") or include("id not found"))
    }
  }
}
