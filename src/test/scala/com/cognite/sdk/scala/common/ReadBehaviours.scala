package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{Id, SttpBackend}
import org.scalatest.{FlatSpec, Matchers}
import io.circe.{Decoder, Encoder}

trait ReadBehaviours extends Matchers { this: FlatSpec =>
  def readable[R, InternalId, PrimitiveId](
      readable: Readable[R, Id]
  )(
      implicit sttpBackend: SttpBackend[Id, _],
      auth: Auth,
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]]
  ): Unit = {
    it should "read items" in {
      readable.read().unsafeBody.items should not be empty
    }

    it should "read items with limit" in {
      (readable.readWithLimit(1).unsafeBody.items should have).length(1)
      (readable.readWithLimit(2).unsafeBody.items should have).length(2)
    }

    it should "read all items" in {
      val first1Length = readable.readAllWithLimit(1).map(_.unsafeBody.length).sum
      first1Length should be(1)
      val first2Length = readable.readAllWithLimit(2).map(_.unsafeBody.length).sum
      first2Length should be(2)
      val allLength = readable.readAllWithLimit(3).map(_.unsafeBody.length).sum
      allLength should be(3)
    }
  }

  // scalastyle:off
  def readableWithRetrieve[R <: WithId[Long], W](
      readable: Readable[R, Id]
        with RetrieveByIds[R, Id],
      idsThatDoNotExist: Seq[Long],
      supportsMissingAndThrown: Boolean
  )(
      implicit sttpBackend: SttpBackend[Id, _],
      auth: Auth,
      errorDecoder: Decoder[CdpApiError],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      itemsDecoder: Decoder[Items[R]],
      d1: Encoder[Items[CogniteId]]
  ): Unit = {
    it should "support retrieving items by id" in {
      val firstTwoItemIds = readable.readWithLimit(2).unsafeBody.items.map(_.id)
      firstTwoItemIds should have size 2
      val maybeItemsRead = readable.retrieveByIds(firstTwoItemIds).unsafeBody
      val itemsReadIds = maybeItemsRead.map(_.id)
      itemsReadIds should have size firstTwoItemIds.size.toLong
      itemsReadIds should contain theSameElementsAs firstTwoItemIds
    }

    it should "return information about missing ids" in {
      val thrown = the[CdpApiException] thrownBy readable
        .retrieveByIds(idsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        val itemsNotFound = thrown.missing

        val notFoundIds = itemsNotFound.get.flatMap(jsonObj =>
          jsonObj("id").get.asNumber.get.toLong)
        notFoundIds should have size idsThatDoNotExist.size.toLong
        notFoundIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy readable
        .retrieveByIds(sameIdsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        sameIdsThrown.missing match {
          case Some(missingItems) =>
            val sameNotFoundIds = missingItems.flatMap(jsonObj =>
              jsonObj("id").get.asNumber.get.toLong).toSet
            // it's a bit funny that the same missing ids are returned duplicated,
            // but that's how it works as of 2019-06-02.
            //sameNotFoundIds should have size sameIdsThatDoNotExist.size.toLong
            sameNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          case None =>
            val duplicatedNotFoundIds = sameIdsThrown.duplicated.get.flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong).toSet
            //duplicatedNotFoundIds should have size sameIdsThatDoNotExist.toSet.size.toLong
            duplicatedNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
        }
      }
    }
  }
}
