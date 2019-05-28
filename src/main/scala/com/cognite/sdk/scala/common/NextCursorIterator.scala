package com.cognite.sdk.scala.common

import com.softwaremill.sttp._

abstract class NextCursorIterator[A, R[_]](firstCursor: Option[String])(
    implicit sttpBackend: SttpBackend[R, _]
) extends Iterator[R[Seq[A]]] {
  // scalafix:off DisableSyntax.var
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var nextCursor = firstCursor
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var isFirst = true
  // scalafix:on

  def get(cursor: Option[String]): R[Response[ItemsWithCursor[A]]]

  override def hasNext: Boolean =
    isFirst || nextCursor.isDefined

  override def next(): R[Seq[A]] = {
    isFirst = false
    sttpBackend.responseMonad.map(get(nextCursor)) { r =>
      r.body.fold(
        // TODO: Figure out how to do this properly using sttpBackend.responseMonad.error or similar
        error => throw new RuntimeException(error), // scalafix:ok
        itemsWithCursor => {
          nextCursor = itemsWithCursor.nextCursor
          itemsWithCursor.items
        }
      )
    }
  }
}
