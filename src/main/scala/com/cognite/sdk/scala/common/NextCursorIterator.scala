package com.cognite.sdk.scala.common

import com.softwaremill.sttp._

@SuppressWarnings(Array("org.wartremover.warts.Var"))
abstract class NextCursorIterator[A, F[_]](
    firstCursor: Option[String],
    limit: Option[Long],
    sttpBackend: SttpBackend[F, _]
) extends Iterator[F[A]] {
  // scalafix:off DisableSyntax.var
  private val m = sttpBackend.responseMonad
  private var nextCursor = firstCursor
  private var isFirst = true
  private var remainingItemsToFetch = limit
  private var bufferSize = 0
  private var bufferedItems = m.unit(Vector.empty[A])
  private var currentBufferedItem: Int = 0
  // scalafix:on

  def get(cursor: Option[String], remainingItems: Option[Long]): F[ItemsWithCursor[A]]

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  private def refillBuffer(): Unit = {
    bufferedItems = m.map(get(nextCursor, remainingItemsToFetch)) { itemsWithCursor =>
      nextCursor = itemsWithCursor.nextCursor
      currentBufferedItem = 0
      val items = itemsWithCursor.items.toVector
      bufferSize = items.length
      remainingItemsToFetch = remainingItemsToFetch.map(_ - bufferSize)
      items
    }
    ()
  }

  override def hasNext: Boolean =
    if (isFirst || (currentBufferedItem >= bufferSize && (nextCursor.isDefined && remainingItemsToFetch
        .getOrElse(1L) > 0))) {
      refillBuffer()
      isFirst = false
      bufferSize > 0
    } else {
      currentBufferedItem < bufferSize
    }

  override def next(): F[A] =
    sttpBackend.responseMonad.map(bufferedItems) { items =>
      currentBufferedItem += 1
      items(currentBufferedItem - 1)
    }
}
