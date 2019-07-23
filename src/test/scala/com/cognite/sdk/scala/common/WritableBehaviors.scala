package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Id
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import org.scalatest.{FlatSpec, Matchers}

trait WritableBehaviors extends Matchers { this: FlatSpec =>
  // scalastyle:off
  def writable[R <: WithId[PrimitiveId], W, PrimitiveId](
      writable: Create[R, W, Id]
        with DeleteByIds[Id, PrimitiveId],
      readExamples: Seq[R],
      createExamples: Seq[W],
      idsThatDoNotExist: Seq[PrimitiveId],
      supportsMissingAndThrown: Boolean
  )(
      implicit errorDecoder: Decoder[CdpApiError],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      itemsDecoder: Decoder[Items[R]],
      itemsEncoder: Encoder[Items[W]],
      d1: Encoder[Items[CogniteId]],
      t: Transformer[R, W]
  ): Unit = {
    it should "be an error to delete using ids that does not exist" in {
      val thrown = the[CdpApiException] thrownBy writable
        .deleteByIds(idsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        val missingIds = thrown.missing.getOrElse(Seq.empty).flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
        missingIds should have size idsThatDoNotExist.size.toLong
        missingIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy writable
        .deleteByIds(sameIdsThatDoNotExist)
        .unsafeBody
      if (supportsMissingAndThrown) {
        // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
        // if duplicated ids that do not exist are specified.
        val sameMissingIds = sameIdsThrown.duplicated match {
          case Some(duplicatedIds) => duplicatedIds.flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
          case None => sameIdsThrown.missing.getOrElse(Seq.empty).flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
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
      val deleteSingle = writable.deleteByIds(createdItem.map(_.id))
      deleteSingle.isSuccess should be(true)

      // create multiple items
      val createdItems = writable.create(readExamples).unsafeBody
      createdItems should have size readExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size readExamples.size.toLong
      val delete = writable.deleteByIds(createdIds)
      delete.isSuccess should be(true)
    }

    it should "create and delete items using the create class" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1)).unsafeBody
      createdItem should have size 1
      createdItem.head.id should not be 0
      val deleteSingle = writable.deleteByIds(createdItem.map(_.id))
      deleteSingle.isSuccess should be(true)

      // create multiple items
      val createdItems = writable.create(createExamples).unsafeBody
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size createExamples.size.toLong
      val delete = writable.deleteByIds(createdIds)
      delete.isSuccess should be(true)
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def updatable[R <: WithId[Long], W, U <: WithId[Long]](
      updatable: Create[R, W, Id]
        with DeleteByIds[Id, Long]
        with Update[R, U, Id]
        with RetrieveByIds[R, Id],
      readExamples: Seq[R],
      updateExamples: Seq[R],
      updateId: (Long, R) => R,
      compareItems: (R, R) => Boolean,
      compareUpdated: (Seq[R], Seq[R]) => Unit
  )(
      implicit errorDecoder: Decoder[CdpApiError],
      itemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      itemsDecoder: Decoder[Items[R]],
      itemsEncoder: Encoder[Items[W]],
      itemsUpdateEncoder: Encoder[Items[U]],
      updateEncoder: Encoder[U],
      d1: Encoder[Items[CogniteId]],
      t: Transformer[R, W],
      t2: Transformer[R, U]
  ): Unit =
    it should "allow updates using the read class" in {
      // create items
      val createdItems = updatable.create(readExamples).unsafeBody
      assert(createdItems.size == readExamples.size)
      createdItems.map(_.id) should not contain 0

      val readItems = updatable.retrieveByIds(createdItems.map(_.id)).unsafeBody

      // update the item to current values
      val unchangedUpdatedItems = updatable.update(readItems).unsafeBody
      assert(unchangedUpdatedItems.size == readItems.size)
      assert(unchangedUpdatedItems.zip(readItems).forall { case (updated, read) =>
        compareItems(updated, read)
      })

      // update the item with new values
      val updates = updateExamples.zip(readItems).map { case (updated, read) => updateId(read.id, updated) }
      val updatedItems = updatable.update(updates).unsafeBody
      assert(updatedItems.size == readItems.size)
      compareUpdated(readItems, updatedItems)

      // delete it
      val deleteSingle = updatable.deleteByIds(createdItems.map(_.id))
      deleteSingle.isSuccess should be(true)
    }

//    it should "create and delete items using the create class" in {
//      // create a single item
//      val createdItem = updatable.create(createExamples.take(1)).unsafeBody
//      createdItem should have size 1
//      createdItem.head.id should not be 0
//      val deleteSingle = updatable.deleteByIds(createdItem.map(_.id))
//      deleteSingle.isSuccess should be (true)
//
//      // create multiple items
//      val createdItems = updatable.create(createExamples).unsafeBody
//      createdItems should have size createExamples.size.toLong
//      val createdIds = createdItems.map(_.id)
//      createdIds should have size createExamples.size.toLong
//      val delete = updatable.deleteByIds(createdIds)
//      delete.isSuccess should be (true)
//    }
  // TODO: test creating multiple items with the same external
  //       id in the same api call for V1
}
