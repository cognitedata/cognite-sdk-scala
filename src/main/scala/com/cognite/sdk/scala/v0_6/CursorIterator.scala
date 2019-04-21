package com.cognite.sdk.scala.v0_6

import com.softwaremill.sttp._

abstract case class CursorIterator[A, R[_]](firstCursor: Option[String])(
    implicit sttpBackend: SttpBackend[R, _])
    extends Iterator[R[Seq[A]]] {
  private var nextCursor = firstCursor
  private var isFirst = true

  def get(cursor: Option[String]): R[Response[ItemsWithCursor[A]]]

  override def hasNext: Boolean =
    isFirst || nextCursor.isDefined

  override def next(): R[Seq[A]] = {
    isFirst = false
    sttpBackend.responseMonad.map(get(nextCursor)) { r =>
      r.body.fold(
        error => throw new RuntimeException(error),
        itemsWithCursor => {
          nextCursor = itemsWithCursor.nextCursor
          itemsWithCursor.items
        }
      )
    }
  }
}
