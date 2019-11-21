package com.cognite.sdk.scala.common

import cats.Applicative
import cats.effect.Concurrent
import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._

final case class FilterRequest[T](
    filter: T,
    limit: Option[Int],
    cursor: Option[String],
    partition: Option[String],
    aggregatedProperties: Option[Seq[String]]
)

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUri {
  private[sdk] def filterWithCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[R]]

  private[sdk] def filterWithNextCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Int],
      aggregatedProperties: Option[Seq[String]]
  ): Stream[F, R] =
    Readable
      .pullFromCursor(cursor, limit, None, filterWithCursor(filter, _, _, _, aggregatedProperties))
      .stream

  def filter(filter: Fi, limit: Option[Int] = None): Stream[F, R] =
    filterWithNextCursor(filter, None, limit, None)
}

trait PartitionedFilterF[R, Fi, F[_]] extends Filter[R, Fi, F] {
  def filterPartitionsF(filter: Fi, numPartitions: Int, limitPerPartition: Option[Int] = None)(
      implicit F: Applicative[F]
  ): F[Seq[Stream[F, R]]]

  def filterConcurrently(
      filter: Fi,
      numPartitions: Int,
      limitPerPartition: Option[Int] = None
  )(
      implicit F: Concurrent[F]
  ): Stream[F, R] =
    Stream
      .eval(filterPartitionsF(filter, numPartitions, limitPerPartition))
      .flatMap(_.fold(Stream.empty)(_.merge(_)))
}

trait PartitionedFilter[R, Fi, F[_]] extends PartitionedFilterF[R, Fi, F] {
  def filterPartitions(
      filter: Fi,
      numPartitions: Int,
      limitPerPartition: Option[Int] = None
  ): Seq[Stream[F, R]] =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursor(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          filterWithCursor(filter, _, _, _, None)
        )
        .stream
    }

  override def filterPartitionsF(
      filter: Fi,
      numPartitions: Int,
      limitPerPartition: Option[Int]
  )(implicit F: Applicative[F]): F[Seq[Stream[F, R]]] =
    F.pure(filterPartitions(filter, numPartitions, limitPerPartition))
}

object Filter {
  //scalastyle:off parameter.number
  def filterWithCursor[F[_], R, Fi: Encoder](
      requestSession: RequestSession[F],
      baseUri: Uri,
      filter: Fi,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  )(
      implicit readItemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      filterRequestEncoder: Encoder[FilterRequest[Fi]]
  ): F[ItemsWithCursor[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    // avoid sending aggregatedProperties to resources that do not support it
    implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    val body =
      FilterRequest(filter, limit, cursor, partition.map(_.toString), aggregatedProperties).asJson
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/list")
          .body(
            // this is an ugly hack necessary because Files does not allow "partition": null
            partition.map(_ => body).getOrElse(body.mapObject(o => o.remove("partition")))
          )
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) => value
          }
      }
  }
  //scalastyle:off parameter.number
}
