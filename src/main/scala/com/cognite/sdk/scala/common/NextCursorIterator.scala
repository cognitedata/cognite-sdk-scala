package com.cognite.sdk.scala.common

import com.softwaremill.sttp._

abstract class NextCursorIterator[A, R[_]](firstCursor: Option[String], limit: Option[Long])(
    implicit sttpBackend: SttpBackend[R, _]
) extends Iterator[R[Seq[A]]] {
  // scalafix:off DisableSyntax.var
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var nextCursor = firstCursor
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var isFirst = true
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var remainingItems = limit
  // scalafix:on

  def get(cursor: Option[String], remainingItems: Option[Long]): R[Response[ItemsWithCursor[A]]]

  override def hasNext: Boolean =
    (isFirst || nextCursor.isDefined) && remainingItems.getOrElse(1L) > 0

  override def next(): R[Seq[A]] = {
    isFirst = false
    sttpBackend.responseMonad.map(get(nextCursor, remainingItems)) { r =>
      r.body.fold(
        // TODO: Figure out how to do this properly using sttpBackend.responseMonad.error or similar
        error => throw new RuntimeException(error), // scalafix:ok
        itemsWithCursor => {
          nextCursor = itemsWithCursor.nextCursor
          remainingItems = remainingItems.map(_ - itemsWithCursor.items.length)
          itemsWithCursor.items
        }
      )
    }
  }
}
