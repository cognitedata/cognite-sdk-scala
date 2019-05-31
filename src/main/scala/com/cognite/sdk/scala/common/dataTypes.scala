package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Uri

final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class CdpApiErrorPayload(code: Int, message: String)
final case class Error[A](error: A)
final case class CdpApiException(url: Uri, code: Int, message: String)
  extends Throwable(s"Request to ${url.toString()} failed with status $code: $message")
final case class CogniteId(id: Long)

trait Extractor[C[_]] {
  def extract[A](c: C[A]): A
}

object Extract {
  def extract[C[_], A](value: C[A])(implicit e: Extractor[C]): A =
    e.extract(value)
}
