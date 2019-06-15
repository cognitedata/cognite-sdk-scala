package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v06.Data
import com.softwaremill.sttp.{Id, Uri}
import io.circe.Decoder
import io.scalaland.chimney.dsl._

final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class CdpApiErrorPayload[A](
    code: Int,
    message: String,
    missing: Option[Seq[A]],
    duplicated: Option[Seq[A]],
    missingFields: Option[Seq[Map[String, String]]]
)
final case class CdpApiError[A](error: CdpApiErrorPayload[A]) {
  def asException(url: Uri): CdpApiException[A] =
    this.error
      .into[CdpApiException[A]]
      .withFieldConst(_.url, url)
      .transform
}
final case class CdpApiException[A](
    url: Uri,
    code: Int,
    message: String,
    missing: Option[Seq[A]],
    duplicated: Option[Seq[A]]
) extends Throwable(s"Request to ${url.toString()} failed with status $code: $message")
final case class CogniteId(id: Long)

final case class DataPoint(
    timestamp: Long,
    value: Double
)

final case class StringDataPoint(
    timestamp: Long,
    value: String
)

trait WithId[I] {
  val id: I
}

trait Extractor[C[_]] {
  def extract[A](c: C[A]): A
}

object ExtractorInstances {
  implicit val idExtractor: Extractor[Id] = new Extractor[Id] {
    override def extract[A](c: Id[A]): A = c
  }
  implicit val dataExtractor: Extractor[Data] = new Extractor[Data] {
    override def extract[A](c: Data[A]): A = c.data
  }
}

object Extract {
  def extract[C[_], A](value: C[A])(implicit e: Extractor[C]): A =
    e.extract(value)
}

object EitherDecoder {
  def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l.or(r)
  }
}
