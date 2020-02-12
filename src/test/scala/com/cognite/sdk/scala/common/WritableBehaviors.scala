package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Id
import io.scalaland.chimney.Transformer
import org.scalatest.{FlatSpec, Matchers}

trait WritableBehaviors extends Matchers { this: FlatSpec =>
  // scalastyle:off
  def writable[R <: WithId[PrimitiveId], W, PrimitiveId](
      writable: Create[R, W, Id] with CreateOne[R, W, Id] with DeleteByIds[Id, PrimitiveId],
      readExamples: Seq[R],
      createExamples: Seq[W],
      idsThatDoNotExist: Seq[PrimitiveId],
      supportsMissingAndThrown: Boolean
  )(implicit t: Transformer[R, W]): Unit = {
    it should "be an error to delete using ids that does not exist" in {
      val thrown = the[CdpApiException] thrownBy writable
        .deleteByIds(idsThatDoNotExist)
      if (supportsMissingAndThrown) {
        val missingIds = thrown.missing
          .getOrElse(Seq.empty)
          .flatMap(jsonObj => jsonObj("id").get.asNumber.get.toLong)
        missingIds should have size idsThatDoNotExist.size.toLong
        missingIds should contain theSameElementsAs idsThatDoNotExist
      }

      val sameIdsThatDoNotExist = Seq(idsThatDoNotExist.head, idsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy writable
        .deleteByIds(sameIdsThatDoNotExist)
      if (supportsMissingAndThrown) {
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
    }

    it should "include the request id in all cdp api exceptions" in {
      val caught = intercept[CdpApiException](
        writable.deleteByIds(idsThatDoNotExist)
      )
      assert(caught.requestId.isDefined)
    }

    it should "create and delete items using the read class" in {
      // create a single item
      val createdItem = writable.createFromRead(readExamples.take(1))
      createdItem should have size 1
      createdItem.head.id should not be 0
      writable.deleteByIds(createdItem.map(_.id))

      // create multiple items
      val createdItems = writable.createFromRead(readExamples)
      createdItems should have size readExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size readExamples.size.toLong
      writable.deleteByIds(createdIds)
    }

    it should "create and delete items using the create class" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1))
      createdItem should have size 1
      createdItem.head.id should not be 0
      writable.deleteByIds(createdItem.map(_.id))

      // create multiple items
      val createdItems = writable.create(createExamples)
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.id)
      createdIds should have size createExamples.size.toLong
      writable.deleteByIds(createdIds)
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def writableWithExternalId[R <: WithExternalId, W <: WithExternalId](
      writable: Create[R, W, Id] with DeleteByExternalIds[Id],
      readExamples: Seq[R],
      createExamples: Seq[W],
      externalIdsThatDoNotExist: Seq[String],
      supportsMissingAndThrown: Boolean
  )(implicit t: Transformer[R, W]): Unit = {
    it should "be an error to delete using external ids that does not exist" in {
      val thrown = the[CdpApiException] thrownBy writable
        .deleteByExternalIds(externalIdsThatDoNotExist)
      if (supportsMissingAndThrown) {
        val missingIds = thrown.missing
          .getOrElse(Seq.empty)
          .flatMap(jsonObj => jsonObj("externalId").get.asString)
        missingIds should have size externalIdsThatDoNotExist.size.toLong
        missingIds should contain theSameElementsAs externalIdsThatDoNotExist
      }

      val sameIdsThatDoNotExist =
        Seq(externalIdsThatDoNotExist.head, externalIdsThatDoNotExist.head)
      val sameIdsThrown = the[CdpApiException] thrownBy writable
        .deleteByExternalIds(sameIdsThatDoNotExist)
      if (supportsMissingAndThrown) {
        // as of 2019-06-03 we're inconsistent about our use of duplicated vs missing
        // if duplicated ids that do not exist are specified.
        val sameMissingIds = sameIdsThrown.duplicated match {
          case Some(duplicatedIds) =>
            duplicatedIds.flatMap(jsonObj => jsonObj("externalId").get.asString)
          case None =>
            sameIdsThrown.missing
              .getOrElse(Seq.empty)
              .flatMap(jsonObj => jsonObj("externalId").get.asString)
        }
        sameMissingIds should have size sameIdsThatDoNotExist.toSet.size.toLong
        sameMissingIds should contain theSameElementsAs sameIdsThatDoNotExist.toSet
      }
    }

    it should "create and delete items using the read class and external ids" in {
      // create a single item
      val createdItem = writable.createFromRead(readExamples.take(1))
      createdItem should have size 1
      createdItem.head.externalId should not be empty
      writable.deleteByExternalIds(createdItem.map(_.externalId.get))

      // create multiple items
      val createdItems = writable.createFromRead(readExamples)
      createdItems should have size readExamples.size.toLong
      val createdExternalIds = createdItems.map(_.externalId.get)
      createdExternalIds should have size readExamples.size.toLong
      writable.deleteByExternalIds(createdExternalIds)
    }

    it should "create and delete items using the create class and external ids" in {
      // create a single item
      val createdItem = writable.create(createExamples.take(1))
      createdItem should have size 1
      createdItem.head.externalId should not be empty
      writable.deleteByExternalIds(createdItem.map(_.externalId.get))

      // create multiple items
      val createdItems = writable.create(createExamples)
      createdItems should have size createExamples.size.toLong
      val createdIds = createdItems.map(_.externalId.get)
      createdIds should have size createExamples.size.toLong
      writable.deleteByExternalIds(createdIds)
    }
    // TODO: test creating multiple items with the same external
    //       id in the same api call for V1
  }

  def updatable[R <: WithId[Long], W, U](
      updatable: CreateOne[R, W, Id]
        with DeleteByIds[Id, Long]
        with UpdateById[R, U, Id]
        with RetrieveByIds[R, Id],
      readExamples: Seq[R],
      updateExamples: Seq[R],
      updateId: (Long, R) => R,
      compareItems: (R, R) => Boolean,
      compareUpdated: (Seq[R], Seq[R]) => Unit
  )(implicit t: Transformer[R, W], t2: Transformer[R, U]): Unit =
    it should "allow updates using the read class" in {
      // create items
      val createdIds = readExamples.map(e => updatable.createOneFromRead(e).id)
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
      updatable.deleteByIds(createdIds)
    }

  def updatableByExternalId[R <: WithExternalId, W, U](
    resource: Create[R, W, Id]
      with DeleteByExternalIds[Id]
      with UpdateByExternalId[R, U, Id]
      with RetrieveByIds[R, Id],
    itemsToCreate: Seq[R],
    updatesToMake: Map[String, U],
    expectedBehaviors: (Seq[R], Seq[R]) => Unit)
  (implicit t: Transformer[R, W]): Unit =
    it should "allow updating by externalId" in {
      val createdItems = resource.createFromRead(itemsToCreate)
      val updatedItems = resource.updateByExternalId(updatesToMake)
      expectedBehaviors(createdItems, updatedItems)
      resource.deleteByExternalIds(updatedItems.map(_.externalId.get))
    }

  def updatableById[R <: WithId[Long], W, U](
    resource: Create[R, W, Id]
      with DeleteByIds[Id, Long]
      with UpdateById[R, U, Id]
      with RetrieveByIds[R, Id],
    itemsToCreate: Seq[R],
    updatesToMake: Seq[U],
    expectedBehaviors: (Seq[R], Seq[R]) => Unit
  )(implicit t: Transformer[R, W]): Unit =
    it should "allow updating by Id" in {
      val createdItems = resource.createFromRead(itemsToCreate)
      val updateMap = createdItems.indices.map(i => (createdItems(i).id, updatesToMake(i))).toMap
      val updatedItems = resource.updateById(updateMap)
      expectedBehaviors(createdItems, updatedItems)
      resource.deleteByIds(updatedItems.map(_.id))
    }

  def deletableWithIgnoreUnknownIds[R <: WithId[Long], W, PrimitiveId](
    writable: Create[R, W, Id] with DeleteByIdsWithIgnoreUnknownIds[Id, Long] with RetrieveByIds[R, Id],
    readExamples: Seq[R],
    idsThatDoNotExist: Seq[Long]
  )(implicit t: Transformer[R, W]): Unit = {
    it should "support ignoring unknown ids on delete" in {
      val createdItems: Id[Seq[R]] = writable.createFromRead(readExamples)
      val createdIds = createdItems.map(_.id)
      val createdAndSomeNonExistingIds = createdIds ++ idsThatDoNotExist
      writable.deleteByIds(createdAndSomeNonExistingIds, ignoreUnknownIds = true)
      val retrieveDeletedException = intercept[CdpApiException](writable.retrieveByIds(createdIds))
      assert(retrieveDeletedException.code == 400)
      val doNotIgnoreException = intercept[CdpApiException](
        writable.deleteByIds(idsThatDoNotExist, ignoreUnknownIds = false))
      assert(doNotIgnoreException.code == 400)
    }
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
