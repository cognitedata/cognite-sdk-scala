package com.cognite.sdk.scala.common

import cats.effect.Concurrent
import com.cognite.sdk.scala.v1.RequestSession
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import fs2._
import io.circe.{Decoder, Encoder}
import io.circe.syntax._

final case class FilterRequest[T](
    filter: T,
    limit: Option[Long],
    cursor: Option[String],
    partition: Option[String]
)

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUri {
  private[sdk] def filterWithCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  ): F[ItemsWithCursor[R]]

  private def filterWithNextCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long]
  ): Stream[F, R] =
    Readable
      .pullFromCursorWithLimit(cursor, limit, None, filterWithCursor(filter, _, _, _))
      .stream

  def filter(filter: Fi): Stream[F, R] =
    filterWithNextCursor(filter, None, None)

  def filterWithLimit(filter: Fi, limit: Long): Stream[F, R] =
    filterWithNextCursor(filter, None, Some(limit))
}

trait PartitionedFilter[R, Fi, F[_]] extends Filter[R, Fi, F] {
  private def filterPartitionsMaybeWithLimit(
      filter: Fi,
      numPartitions: Int,
      limitPerPartition: Option[Long]
  ) =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursorWithLimit(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          filterWithCursor(filter, _, _, _)
        )
        .stream
    }

  def filterPartitions(filter: Fi, numPartitions: Int): Seq[Stream[F, R]] =
    filterPartitionsMaybeWithLimit(filter, numPartitions, None)

  def filterPartitionsWithLimit(
      filter: Fi,
      numPartitions: Int,
      limitPerPartition: Long
  ): Seq[Stream[F, R]] =
    filterPartitionsMaybeWithLimit(filter, numPartitions, Some(limitPerPartition))

  def filterConcurrently(filter: Fi, numPartitions: Int)(implicit c: Concurrent[F]): Stream[F, R] =
    filterPartitions(filter, numPartitions).fold(Stream.empty)(_.merge(_))

  def filterConcurrentlyWithLimit(filter: Fi, numPartitions: Int, limitPerPartition: Long)(
      implicit c: Concurrent[F]
  ): Stream[F, R] =
    filterPartitionsWithLimit(filter, numPartitions, limitPerPartition).fold(Stream.empty)(
      _.merge(_)
    )
}

object Filter {
  def filterWithCursor[F[_], R, Fi: Encoder](
      requestSession: RequestSession[F],
      baseUri: Uri,
      filter: Fi,
      cursor: Option[String],
      limit: Option[Long],
      partition: Option[Partition]
  )(
      implicit readItemsWithCursorDecoder: Decoder[ItemsWithCursor[R]],
      filterRequestEncoder: Encoder[FilterRequest[Fi]]
  ): F[ItemsWithCursor[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    val body = FilterRequest(filter, limit, cursor, partition.map(_.toString)).asJson
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
}
