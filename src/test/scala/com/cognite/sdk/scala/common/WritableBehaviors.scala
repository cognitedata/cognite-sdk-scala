// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.Id
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait WritableBehaviors extends Matchers with OptionValues { this: AnyFlatSpec =>
  // scalastyle:off
  def writable[R <: ToCreate[W] with WithId[PrimitiveId], W, PrimitiveId](
      writable: Create[R, W, Id] with CreateOne[R, W, Id],
      maybeDeletable: Option[DeleteByIds[Id, PrimitiveId]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      idsThatDoNotExist: Seq[PrimitiveId],
      supportsMissingAndThrown: Boolean
  ): Unit = {
    it should "be an error to delete using ids that does not exist" in {
      maybeDeletable.map { deletable =>
        val thrown = the[CdpApiException] thrownBy deletable.deleteByIds(idsThatDoNotExist)
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
        )
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
          sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
        }
      }

    }

    it should "include the request id in all cdp api exceptions" in {
      maybeDeletable.map { deletable =>
        val caught = intercept[CdpApiException](deletable.deleteByIds(idsThatDoNotExist))
        assert(caught.requestId.isDefined)
      }
    }

    it should "create and delete items using the read class" in {
      // create a single item
      val createdItem = writable.createFromRead(readExamples.take(1))
      createdItem should have size 1
      createdItem.headOption.value.id should not be 0

      maybeDeletable.map(_.deleteByIds(createdItem.map(_.id)))

      // create multiple items
      val createdItems = writable.createFromRead(readExamples)
      createdItems should have size readExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size readExamples.size.toLong

      maybeDeletable.map(_.deleteByIds(createdIds))
    }

    it should "create and delete items using the create class" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1))
      createdItem should have size 1
      createdItem.headOption.value.id should not be 0

      maybeDeletable.map(_.deleteByIds(createdItem.map(_.id)))

      // create multiple items
      val createdItems = writable.create(createExamples)
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size createExamples.size.toLong

      maybeDeletable.map(_.deleteByIds(createdIds))
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def writableWithExternalId[R <: ToCreate[W] with WithExternalIdGeneric[Option], W <: WithExternalIdGeneric[
    Option
  ]](
      writable: Create[R, W, Id],
      maybeDeletable: Option[DeleteByExternalIds[Id]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean
  ): Unit =
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
      writable: Create[R, W, Id],
      maybeDeletable: Option[DeleteByExternalIds[Id]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean,
      trySameIdsThatDoNotExist: Boolean = true
  ): Unit =
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
      writable: Create[R, W, Id],
      maybeDeletable: Option[DeleteByExternalIds[Id]],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean,
      trySameIdsThatDoNotExist: Boolean = true
  ): Unit = {
    it should "be an error to delete using external ids that does not exist" in {
      maybeDeletable.map { deletable =>
        val thrown = the[CdpApiException] thrownBy deletable.deleteByExternalIds(
          externalIdsThatDoNotExist
        )
        if (supportsMissingAndThrown) {
          val missingIds = thrown.missing
            .getOrElse(Seq.empty)
            .map(jsonObj => jsonObj("externalId").value.asString.value)
          missingIds should have size externalIdsThatDoNotExist.size.toLong
          missingIds should contain theSameElementsAs externalIdsThatDoNotExist
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
          )
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
            sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
          }
        }
      }
    }

    it should "create and delete items using the read class and external ids" in {
      val createdItems = writable.createFromRead(readExamples)
      createdItems should have size readExamples.size.toLong
      val createdExternalIds = createdItems.map(_.getExternalId().value)
      createdExternalIds should have size readExamples.size.toLong
      maybeDeletable.map(_.deleteByExternalIds(createdExternalIds))
    }

    it should "create and delete items using the create class and external ids" in {
      // create multiple items
      val createdItems = writable.create(createExamples)
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.getExternalId().value)
      createdIds should have size createExamples.size.toLong
      maybeDeletable.map(_.deleteByExternalIds(createdIds))
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def updatable[R <: ToCreate[W] with ToUpdate[U] with WithId[Long], W, U](
      updatable: Create[R, W, Id] with UpdateById[R, U, Id] with RetrieveByIds[R, Id],
      maybeDeletable: Option[DeleteByIds[Id, Long]],
      readExamples: Seq[R],
      updateExamples: Seq[R],
      updateId: (Long, R) => R,
      compareItems: (R, R) => Boolean,
      compareUpdated: (Seq[R], Seq[R]) => Unit
  ): Unit =
    it should "allow updates using the read class" in {
      // create items
      val createdIds = updatable.createFromRead(readExamples).map(_.id)
      assert(createdIds.size == readExamples.size)
      createdIds should not contain 0

      val readItems = updatable.retrieveByIds(createdIds)

      // update the item to current values
      val unchangedUpdatedItems = updatable.updateFromRead(readItems)
      assert(unchangedUpdatedItems.size == readItems.size)
      assert(unchangedUpdatedItems.zip(readItems).forall {
        case (updated, read) =>
          compareItems(updated, read)
      })

      // update the item with new values
      val updates =
        updateExamples.zip(readItems).map { case (updated, read) => updateId(read.id, updated) }
      val updatedItems = updatable.updateFromRead(updates)
      assert(updatedItems.size == readItems.size)
      compareUpdated(readItems, updatedItems)

      // delete it
      maybeDeletable.map(_.deleteByIds(createdIds))
    }

  def updatableByExternalId[R <: ToCreate[W] with WithExternalId, W, U](
      resource: Create[R, W, Id] with UpdateByExternalId[R, U, Id] with RetrieveByIds[R, Id],
      maybeDeletable: Option[DeleteByExternalIds[Id]],
      itemsToCreate: Seq[R],
      updatesToMake: Map[String, U],
      expectedBehaviors: (Seq[R], Seq[R]) => Unit
  ): Unit =
    it should "allow updating by externalId" in {
      val createdItems = resource.createFromRead(itemsToCreate)
      val updatedItems = resource.updateByExternalId(updatesToMake)
      expectedBehaviors(createdItems, updatedItems)

      maybeDeletable.map(_.deleteByExternalIds(updatedItems.map(_.externalId.value)))
    }

  def updatableById[R <: ToCreate[W] with ToUpdate[U] with WithId[Long], W, U](
      resource: Create[R, W, Id] with UpdateById[R, U, Id] with RetrieveByIds[R, Id],
      maybeDeletable: Option[DeleteByIds[Id, Long]],
      itemsToCreate: Seq[R],
      updatesToMake: Seq[U],
      expectedBehaviors: (Seq[R], Seq[R]) => Unit
  ): Unit =
    it should "allow updating by Id" in {
      val createdItems = resource.createFromRead(itemsToCreate)
      val updateMap = createdItems.indices.map(i => (createdItems(i).id, updatesToMake(i))).toMap
      val updatedItems = resource.updateById(updateMap)
      expectedBehaviors(createdItems, updatedItems)

      maybeDeletable.map(_.deleteByIds(updatedItems.map(_.id)))
    }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def updatableByBothIds[R <: ToCreate[W] with ToUpdate[U] with WithId[Long] with WithExternalId, W, U](
    resource: Create[R, W, Id] with UpdateById[R, U, Id] with UpdateByExternalId[R, U, Id] with RetrieveByIds[R, Id],
    maybeDeletable: Option[DeleteByIds[Id, Long] with DeleteByExternalIds[Id]],
    itemsToCreate: Seq[R],
    updatesToMake: Seq[U],
    expectedBehaviors: (Seq[R], Seq[R]) => Unit
  ): Unit = {
    val externalIdUpdates = updatesToMake.zip(itemsToCreate).map {
      case (u, c) => c.externalId.get -> u
    }.toMap
    updatableById(resource, maybeDeletable, itemsToCreate, updatesToMake, expectedBehaviors)
    updatableByExternalId(resource, maybeDeletable, itemsToCreate, externalIdUpdates, expectedBehaviors)
  }


  def deletableWithIgnoreUnknownIds[R <: ToCreate[W] with WithId[Long], W, PrimitiveId](
      writable: Create[R, W, Id]
        with DeleteByIdsWithIgnoreUnknownIds[Id, Long]
        with RetrieveByIds[R, Id],
      readExamples: Seq[R],
      idsThatDoNotExist: Seq[Long]
  ): Unit =
    it should "support ignoring unknown ids on delete" in {
      val createdItems: Id[Seq[R]] = writable.createFromRead(readExamples)
      val createdIds = createdItems.map(_.id)
      val createdAndSomeNonExistingIds = createdIds ++ idsThatDoNotExist
      writable.deleteByIds(createdAndSomeNonExistingIds, ignoreUnknownIds = true)
      val retrieveDeletedException = intercept[CdpApiException](writable.retrieveByIds(createdIds))
      assert(retrieveDeletedException.code === 400)
      val doNotIgnoreException = intercept[CdpApiException](
        writable.deleteByIds(idsThatDoNotExist)
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
