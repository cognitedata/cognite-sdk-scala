package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Id
import io.scalaland.chimney.Transformer
import org.scalatest.{FlatSpec, Matchers}

trait WritableResourceBehaviors extends Matchers { this: FlatSpec =>
  // scalastyle:off method.length
  def writableResource[R <: WithId, W, C[_], I](
      writable: ReadWritableResource[R, W, Id, C, I],
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
}
