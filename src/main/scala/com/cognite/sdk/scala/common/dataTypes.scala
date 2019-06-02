package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Uri
import io.circe.Decoder
import io.scalaland.chimney.dsl._

final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class CdpApiErrorPayload[A](code: Int, message: String, missing: Option[Seq[A]], duplicated: Option[Seq[A]])
final case class CdpApiError[A](error: CdpApiErrorPayload[A]) {
  def asException(url: Uri): CdpApiException[A] =
    this.error
      .into[CdpApiException[A]]
      .withFieldConst(_.url, url)
      .transform
}
final case class CdpApiException[A](url: Uri, code: Int, message: String, missing: Option[Seq[A]], duplicated: Option[Seq[A]])
  extends Throwable(s"Request to ${url.toString()} failed with status $code: $message")
final case class CogniteId(id: Long)

trait WithId {
  val id: Long
}

trait Extractor[C[_]] {
  def extract[A](c: C[A]): A
}

object Extract {
  def extract[C[_], A](value: C[A])(implicit e: Extractor[C]): A =
    e.extract(value)
}

object Decoders {
  def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l or r
  }
}