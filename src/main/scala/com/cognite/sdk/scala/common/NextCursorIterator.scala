package com.cognite.sdk.scala.common

import com.softwaremill.sttp._

abstract class NextCursorIterator[A, F[_]](
    firstCursor: Option[String],
    limit: Option[Long],
    sttpBackend: SttpBackend[F, _]
) extends Iterator[F[Seq[A]]] {
  // scalafix:off DisableSyntax.var
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var nextCursor = firstCursor
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var isFirst = true
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var remainingItems = limit
  // scalafix:on

  def get(cursor: Option[String], remainingItems: Option[Long]): F[ItemsWithCursor[A]]

  override def hasNext: Boolean =
    (isFirst || nextCursor.isDefined) && remainingItems.getOrElse(1L) > 0

  override def next(): F[Seq[A]] = {
    isFirst = false
    sttpBackend.responseMonad.map(get(nextCursor, remainingItems)) { itemsWithCursor =>
      nextCursor = itemsWithCursor.nextCursor
      remainingItems = remainingItems.map(_ - itemsWithCursor.items.length)
      itemsWithCursor.items
    }
  }
}
