package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Id
import io.scalaland.chimney.Transformer
import org.scalatest.{FlatSpec, Matchers}

trait ReadableResourceBehaviors extends Matchers { this: FlatSpec =>
  // scalastyle:off method.length
  def readableResource[R <: WithId, C[_], I](readable: ReadableResource[R, Id, C, I], supportsMissingAndThrown: Boolean): Unit = {
    it should "read items" in {
      readable.read().unsafeBody.items should not be empty
    }

    it should "read items with limit" in {
      readable.readWithLimit(1).unsafeBody.items should have length 1
      readable.readWithLimit(2).unsafeBody.items should have length 2
    }

    it should "read all items" in {
      val first1Length = readable.readAllWithLimit(1).map(_.length).sum
      first1Length should be (1)
      val first2Length = readable.readAllWithLimit(2).map(_.length).sum
      first2Length should be (2)
      val allLength = readable.readAllWithLimit(3).map(_.length).sum
      allLength should be (3)
    }

    it should "support retrieving items by id" in {
      val firstTwoItemIds = readable.readWithLimit(2).unsafeBody.items.map(_.id)
      firstTwoItemIds should have size 2
      val maybeItemsRead = readable.retrieveByIds(firstTwoItemIds).unsafeBody
      val itemsReadIds = maybeItemsRead.map(_.id)
      itemsReadIds should have size firstTwoItemIds.size.toLong
      itemsReadIds should contain theSameElementsAs firstTwoItemIds
    }

    it should "return information about missing ids" in {
      val idsThatDoNotExist = Seq(999991L, 999992L)
      val thrown = the[CdpApiException[CogniteId]] thrownBy readable
        .retrieveByIds(idsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        val itemsNotFound = thrown.missing
        val notFoundIds = itemsNotFound.get.map(_.id)
        notFoundIds should have size idsThatDoNotExist.size.toLong
        notFoundIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(999991L, 999991L)
      val sameIdsThrown = the[CdpApiException[CogniteId]] thrownBy readable
        .retrieveByIds(sameIdsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        sameIdsThrown.missing match {
          case Some(missingItems) =>
            val sameNotFoundIds = missingItems.map(_.id).toSet
            // it's a bit funny that the same missing ids are returned duplicated,
            // but that's how it works as of 2019-06-02.
            //sameNotFoundIds should have size sameIdsThatDoNotExist.size.toLong
            sameNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          case None =>
            val duplicatedNotFoundIds = sameIdsThrown.duplicated.get.map(_.id).toSet
            //duplicatedNotFoundIds should have size sameIdsThatDoNotExist.toSet.size.toLong
            duplicatedNotFoundIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
        }
      }
    }
  }

  def writableResource[R <: WithId, W, C[_], I](
      writable: WritableResource[R, W, Id, C, I],
      readExamples: Seq[R],
      createExamples: Seq[W],
      supportsMissingAndThrown: Boolean
  )(implicit t: Transformer[R, W]): Unit = {
    it should "be an error to delete using ids that does not exist" in {
      val idsThatDoNotExist = Seq(999991L, 999992L)
      val thrown = the[CdpApiException[CogniteId]] thrownBy writable
        .deleteByIds(idsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        val missingIds = thrown.missing.getOrElse(Seq.empty).map(_.id)
        missingIds should have size idsThatDoNotExist.size.toLong
        missingIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(999991L, 999991L)
      val sameIdsThrown = the[CdpApiException[CogniteId]] thrownBy writable
        .deleteByIds(sameIdsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
        // if duplicated ids that do not exist are specified.
        val sameMissingIds = sameIdsThrown.duplicated match {
          case Some(duplicatedIds) => duplicatedIds.map(_.id)
          case None => sameIdsThrown.missing.getOrElse(Seq.empty).map(_.id)
        }
        sameMissingIds should have size sameIdsThatDoNotExist.toSet.size.toLong
        sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
      }
    }

    it should "create and delete items using the read class" in {
      // create a single item
      val createdItem = writable.create(readExamples.take(1)).unsafeBody
      createdItem should have size 1
      createdItem.head.id should not be 0
      writable.deleteByIds(createdItem.map(_.id)).unsafeBody

      // create multiple items
      val createdItems = writable.create(readExamples).unsafeBody
      createdItems should have size readExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size readExamples.size.toLong
      writable.deleteByIds(createdIds).unsafeBody
    }

    it should "create and delete items using the create class" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1)).unsafeBody
      createdItem should have size 1
      createdItem.head.id should not be 0
      writable.deleteByIds(createdItem.map(_.id)).unsafeBody

      // create multiple items
      val createdItems = writable.create(createExamples).unsafeBody
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size createExamples.size.toLong
      writable.deleteByIds(createdIds).unsafeBody
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }
  // scalastyle:on method.length
}
