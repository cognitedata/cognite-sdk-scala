// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id
import cats.effect.IO
import cats.effect.unsafe.IORuntime

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.SizeIs"))
trait WritableBehaviors extends Matchers with OptionValues { this: AnyFlatSpec =>
  def writable[R <: ToCreate[W] with WithId[PrimitiveId], W, PrimitiveId](
      writable: Create[R, W, IO] with CreateOne[R, W, IO],
      maybeDeletable: Option[DeleteByIds[IO, PrimitiveId]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      idsThatDoNotExist: Seq[PrimitiveId],
      supportsMissingAndThrown: Boolean,
      deleteMissingThrows: Boolean = true
  )(implicit IORuntime: IORuntime): Unit = {
    it should "be an error to delete using ids that does not exist" in {
      maybeDeletable.map { deletable =>
        if (deleteMissingThrows) {
          val thrown = the[CdpApiException] thrownBy deletable.deleteByIds(idsThatDoNotExist).unsafeRunSync()
          if (supportsMissingAndThrown) {
            val missingIds = thrown.missing
              .getOrElse(Seq.empty)
              .map(jsonObj => jsonObj("id").value.asNumber.value.toLong.value)
            missingIds should have size idsThatDoNotExist.size.toLong
            missingIds should contain theSameElementsAs idsThatDoNotExist
          }

          val sameIdsThatDoNotExist = Seq.fill(2)(idsThatDoNotExist(0))
          val sameIdsThrown = the[CdpApiException] thrownBy deletable.deleteByIds(
            sameIdsThatDoNotExist
          ).unsafeRunSync()
          if (supportsMissingAndThrown) {
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
            // TODO: remove when we get rid of the warning
            val _ = sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          }
        }
      }

    }

    it should "include the request id in all cdp api exceptions" in {
      maybeDeletable.map { deletable =>
        if (deleteMissingThrows) {
          val caught = intercept[CdpApiException](deletable.deleteByIds(idsThatDoNotExist).unsafeRunSync())
          // TODO: remove when we get rid of the warning
          val _ = assert(caught.requestId.isDefined)
        }
      }
    }

    it should "create and delete items using the read class" in {
      // create a single item
      val createdItem = writable.createFromRead(readExamples.take(1)).unsafeRunSync()
      createdItem should have size 1
      createdItem.headOption.value.id should not be 0

      maybeDeletable.map(_.deleteByIds(createdItem.map(_.id)).unsafeRunSync())

      // create multiple items
      val createdItems = writable.createFromRead(readExamples).unsafeRunSync()
      createdItems should have size readExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size readExamples.size.toLong

      maybeDeletable.map(_.deleteByIds(createdIds).unsafeRunSync())
    }

    it should "create and delete items using the create class" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1)).unsafeRunSync()
      createdItem should have size 1
      createdItem.headOption.value.id should not be 0

      maybeDeletable.map(_.deleteByIds(createdItem.map(_.id)).unsafeRunSync())

      // create multiple items
      val createdItems = writable.create(createExamples).unsafeRunSync()
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size createExamples.size.toLong

      maybeDeletable.map(_.deleteByIds(createdIds).unsafeRunSync())
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def writableWithExternalId[R <: ToCreate[W] with WithExternalIdGeneric[Option], W <: WithExternalIdGeneric[
    Option
  ]](
      writable: Create[R, W, IO],
      maybeDeletable: Option[DeleteByExternalIds[IO]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean
  )(implicit IORuntime: IORuntime): Unit =
    writableWithExternalIdGeneric[Option, R, W](
      writable,
      maybeDeletable,
      readExamples,
      createExamples,
      externalIdsThatDoNotExist,
      supportsMissingAndThrown
    )

  def writableWithRequiredExternalId[R <: ToCreate[W] with WithExternalIdGeneric[Id], W <: WithExternalIdGeneric[
    Id
  ]](
      writable: Create[R, W, IO],
      maybeDeletable: Option[DeleteByExternalIds[IO]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean,
      trySameIdsThatDoNotExist: Boolean = true
  )(implicit IORuntime: IORuntime): Unit =
    writableWithExternalIdGeneric[Id, R, W](
      writable,
      maybeDeletable,
      readExamples,
      createExamples,
      externalIdsThatDoNotExist,
      supportsMissingAndThrown,
      trySameIdsThatDoNotExist
  )

  def writableWithExternalIdGeneric[A[_], R <: ToCreate[W] with WithExternalIdGeneric[A], W <: WithExternalIdGeneric[
    A
  ]](
      writable: Create[R, W, IO],
      maybeDeletable: Option[DeleteByExternalIds[IO]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean,
      trySameIdsThatDoNotExist: Boolean = true
  )(implicit ioRuntime: IORuntime): Unit = {
    it should "be an error to delete using external ids that does not exist" in {
      maybeDeletable.map { deletable =>
        val thrown = the[CdpApiException] thrownBy deletable.deleteByExternalIds(
          externalIdsThatDoNotExist
        ).unsafeRunSync()
        if (supportsMissingAndThrown) {
          val missingIds = thrown.missing
            .getOrElse(Seq.empty)
            .map(jsonObj => jsonObj("externalId").value.asString.value)
          missingIds should have size externalIdsThatDoNotExist.size.toLong
          // TODO: remove when we get rid of the warning
          val _ = missingIds should contain theSameElementsAs externalIdsThatDoNotExist
        }
        if (trySameIdsThatDoNotExist) {
          // Relationships do not return the items in "missing" if we query for the same missing items within the same request.
          // It only returns a message so we skip this test in that case. I think this could be reported to API developers
          // since it does not follow the same style with the others, you need to experiment to see it.
          // ie. {
          //    "error": {
          //        "code": 400,
          //        "message": "Request had 1 item(s) with one or more constraint violations:
          //        [items is not a distinct set; duplicates: [{\"externalId\":\"AT_Matzen a469\"}]]"
          //    }
          //}
          val sameIdsThatDoNotExist = Seq.fill(2)(externalIdsThatDoNotExist(0))
          val sameIdsThrown = the[CdpApiException] thrownBy deletable.deleteByExternalIds(
            sameIdsThatDoNotExist
          ).unsafeRunSync()
          if (supportsMissingAndThrown) {
            // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
            // if duplicated ids that do not exist are specified.
            val sameMissingIds = sameIdsThrown.duplicated match {
              case Some(duplicatedIds) =>
                duplicatedIds.map(jsonObj => jsonObj("externalId").value.asString.value)
              case None =>
                sameIdsThrown.missing
                  .getOrElse(Seq.empty)
                  .map(jsonObj => jsonObj("externalId").value.asString.value)
            }
            sameMissingIds should have size sameIdsThatDoNotExist.toSet.size.toLong
            // TODO: remove when we get rid of the warning
            val _ = sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          }
        }
      }
    }

    it should "create and delete items using the read class and external ids" in {
      val createdItems = writable.createFromRead(readExamples).unsafeRunSync()
      createdItems should have size readExamples.size.toLong
      val createdExternalIds = createdItems.map(_.getExternalId.value)
      createdExternalIds should have size readExamples.size.toLong
      maybeDeletable.map(_.deleteByExternalIds(createdExternalIds).unsafeRunSync())
    }

    it should "create and delete items using the create class and external ids" in {
      // create multiple items
      val createdItems = writable.create(createExamples).unsafeRunSync()
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.getExternalId.value)
      createdIds should have size createExamples.size.toLong
      maybeDeletable.map(_.deleteByExternalIds(createdIds).unsafeRunSync())
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def updatable[R <: ToCreate[W] with ToUpdate[U] with WithId[Long], W, U](
      updatable: Create[R, W, IO] with UpdateById[R, U, IO] with RetrieveByIds[R, IO],
      maybeDeletable: Option[DeleteByIds[IO, Long]],
      readExamples: Seq[R],
      updateExamples: Seq[R],
      updateId: (Long, R) => R,
      compareItems: (R, R) => Boolean,
      compareUpdated: (Seq[R], Seq[R]) => Unit
  )(implicit ioRuntime: IORuntime): Unit =
    it should "allow updates using the read class" in {
      // create items
      val createdIds = updatable.createFromRead(readExamples).unsafeRunSync().map(_.id)
      assert(createdIds.size == readExamples.size)
      createdIds should not contain 0

      val readItems = updatable.retrieveByIds(createdIds).unsafeRunSync()

      // update the item to current values
      val unchangedUpdatedItems = updatable.updateFromRead(readItems).unsafeRunSync()
      assert(unchangedUpdatedItems.size == readItems.size)
      assert(unchangedUpdatedItems.zip(readItems).forall {
        case (updated, read) =>
          compareItems(updated, read)
      })

      // update the item with new values
      val updates =
        updateExamples.zip(readItems).map { case (updated, read) => updateId(read.id, updated) }
      val updatedItems = updatable.updateFromRead(updates).unsafeRunSync()
      assert(updatedItems.size == readItems.size)
      compareUpdated(readItems, updatedItems)

      // delete it
      maybeDeletable.map(_.deleteByIds(createdIds).unsafeRunSync())
    }

  def updatableByExternalId[R <: ToCreate[W] with WithExternalId, W, U](
      resource: Create[R, W, IO] with UpdateByExternalId[R, U, IO] with RetrieveByIds[R, IO],
      maybeDeletable: Option[DeleteByExternalIds[IO]],
      itemsToCreate: Seq[R],
      updatesToMake: Map[String, U],
      expectedBehaviors: (Seq[R], Seq[R]) => Unit
  )(implicit ioRuntime: IORuntime): Unit =
    it should "allow updating by externalId" in {
      val createdItems = resource.createFromRead(itemsToCreate).unsafeRunSync()
      val updatedItems = resource.updateByExternalId(updatesToMake).unsafeRunSync()
      expectedBehaviors(createdItems, updatedItems)

      maybeDeletable.map(_.deleteByExternalIds(updatedItems.map(_.externalId.value)).unsafeRunSync())
    }

  def updatableByRequiredExternalId[R <: ToCreate[W] with WithRequiredExternalId, W, U](
      resource: Create[R, W, IO] with UpdateByExternalId[R, U, IO] with RetrieveByIds[R, IO],
      maybeDeletable: Option[DeleteByExternalIds[IO]],
      itemsToCreate: Seq[R],
      updatesToMake: Map[String, U],
      expectedBehaviors: (Seq[R], Seq[R]) => Unit
  )(implicit IORuntime: IORuntime): Unit =
    it should "allow updating by externalId" in {
      val createdItems = resource.createFromRead(itemsToCreate).unsafeRunSync()
      val updatedItems = resource.updateByExternalId(updatesToMake).unsafeRunSync()
      expectedBehaviors(createdItems, updatedItems)

      maybeDeletable.map(_.deleteByExternalIds(updatedItems.map(_.externalId)).unsafeRunSync())
    }

  def updatableById[R <: ToCreate[W] with ToUpdate[U] with WithId[Long], W, U](
      resource: Create[R, W, IO] with UpdateById[R, U, IO] with RetrieveByIds[R, IO],
      maybeDeletable: Option[DeleteByIds[IO, Long]],
      itemsToCreate: Seq[R],
      updatesToMake: Seq[U],
      expectedBehaviors: (Seq[R], Seq[R]) => Unit
  )(implicit IORuntime: IORuntime): Unit =
    it should "allow updating by Id" in {
      val createdItems = resource.createFromRead(itemsToCreate).unsafeRunSync()
      val updateMap = createdItems.indices.map(i => (createdItems(i).id, updatesToMake(i))).toMap
      val updatedItems = resource.updateById(updateMap).unsafeRunSync()
      expectedBehaviors(createdItems, updatedItems)

      maybeDeletable.map(_.deleteByIds(updatedItems.map(_.id)).unsafeRunSync())
    }

  def deletableWithIgnoreUnknownIds[R <: ToCreate[W] with WithId[Long], W, PrimitiveId](
      writable: Create[R, W, IO]
        with DeleteByIdsWithIgnoreUnknownIds[IO, Long]
        with RetrieveByIds[R, IO],
      readExamples: Seq[R],
      idsThatDoNotExist: Seq[Long]
  )(implicit IORuntime: IORuntime): Unit =
    it should "support ignoring unknown ids on delete" in {
      val createdItems: IO[Seq[R]] = writable.createFromRead(readExamples)
      val createdIds = createdItems.unsafeRunSync().map(_.id)
      val createdAndSomeNonExistingIds = createdIds ++ idsThatDoNotExist
      writable.deleteByIds(createdAndSomeNonExistingIds, ignoreUnknownIds = true).unsafeRunSync()
      val retrieveDeletedException = intercept[CdpApiException](writable.retrieveByIds(createdIds).unsafeRunSync())
      assert(retrieveDeletedException.code === 400)
      val doNotIgnoreException = intercept[CdpApiException](
        writable.deleteByIds(idsThatDoNotExist).unsafeRunSync()
      )
      assert(doNotIgnoreException.code === 400)
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
