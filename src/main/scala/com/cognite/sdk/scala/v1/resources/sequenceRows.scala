// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.Monad
import cats.implicits._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class SequenceRows[F[_]](val requestSession: RequestSession[F])(implicit F: Monad[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import SequenceRows._
  override val baseUrl = uri"${requestSession.baseUrl}/sequences/data"

  implicit val errorOrItemsSequenceRowsResponseDecoder
      : Decoder[Either[CdpApiError, SequenceRowsResponse]] =
    EitherDecoder.eitherDecoder[CdpApiError, SequenceRowsResponse]
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def insertById(id: Long, columns: Seq[String], rows: Seq[SequenceRow]): F[Unit] =
    requestSession
      .post[Unit, Unit, Items[SequenceRowsInsertById]](
        Items(Seq(SequenceRowsInsertById(id, columns, rows))),
        baseUrl,
        _ => ()
      )

  def insertByExternalId(
      externalId: String,
      columns: Seq[String],
      rows: Seq[SequenceRow]
  ): F[Unit] =
    requestSession
      .post[Unit, Unit, Items[SequenceRowsInsertByExternalId]](
        Items(Seq(SequenceRowsInsertByExternalId(externalId, columns, rows))),
        baseUrl,
        _ => ()
      )

  def deleteById(id: Long, rows: Seq[Long]): F[Unit] =
    requestSession
      .post[Unit, Unit, Items[SequenceRowsDeleteById]](
        Items(Seq(SequenceRowsDeleteById(id, rows))),
        uri"$baseUrl/delete",
        _ => ()
      )

  def deleteByExternalId(externalId: String, rows: Seq[Long]): F[Unit] =
    requestSession
      .post[Unit, Unit, Items[SequenceRowsDeleteByExternalId]](
        Items(Seq(SequenceRowsDeleteByExternalId(externalId, rows))),
        uri"$baseUrl/delete",
        _ => ()
      )

  private def sendQuery(query: SequenceRowsQuery, batchSize: Int) =
    requestSession
      .post[SequenceRowsResponse, SequenceRowsResponse, SequenceRowsQuery](
        query.withCursorAndLimit(
          query.cursor,
          Some(math.min(batchSize, query.limit.getOrElse(batchSize)))
        ),
        uri"$baseUrl/list",
        (v: SequenceRowsResponse) => v
      )

  private def pullQueryResults(
      query: SequenceRowsQuery,
      batchSize: Int
  ): fs2.Pull[F, SequenceRowsResponse, Unit] =
    Readable.pageThroughCursors[F, Option[Int], SequenceRowsResponse](
      query.cursor,
      query.limit,
      (cursor: Option[String], limit: Option[Int]) =>
        if (limit.exists(_ <= 0)) {
          F.pure(Option.empty)
        } else {
          sendQuery(query.withCursorAndLimit(cursor, limit), batchSize)
            .map(r => Some((r, limit.map(_ - r.rows.length))))
        }
    )

  private def pullFollowingItems(
      nextCursor: Option[String],
      firstPageCount: Int,
      query: SequenceRowsQuery,
      batchSize: Int
  ): fs2.Stream[F, SequenceRow] =
    nextCursor match {
      case None => fs2.Stream.empty
      case Some(cursor) =>
        pullQueryResults(
          query.withCursorAndLimit(Some(cursor), query.limit.map(_ - firstPageCount)),
          batchSize
        ).stream.flatMap(r => fs2.Stream.emits(r.rows))
    }

  private def queryColumnsAndStream(
      query: SequenceRowsQuery,
      batchSize: Int
  ): F[(Seq[SequenceColumnSignature], fs2.Stream[F, SequenceRow])] =
    sendQuery(query, batchSize).map(response =>
      (
        response.columns,
        fs2.Stream.emits(response.rows) ++ pullFollowingItems(
          response.nextCursor,
          response.rows.length,
          query,
          batchSize
        )
      )
    )

  def queryById(
      id: Long,
      inclusiveStart: Option[Long],
      exclusiveEnd: Option[Long],
      limit: Option[Int] = None,
      columns: Option[Seq[String]] = None,
      batchSize: Int = Constants.rowsBatchSize
  ): F[(Seq[SequenceColumnSignature], fs2.Stream[F, SequenceRow])] =
    queryColumnsAndStream(
      SequenceRowsQueryById(id, inclusiveStart, exclusiveEnd, limit, None, columns),
      batchSize
    )

  def queryByExternalId(
      externalId: String,
      inclusiveStart: Option[Long],
      exclusiveEnd: Option[Long],
      limit: Option[Int] = None,
      columns: Option[Seq[String]] = None,
      batchSize: Int = Constants.rowsBatchSize
  ): F[(Seq[SequenceColumnSignature], fs2.Stream[F, SequenceRow])] =
    queryColumnsAndStream(
      SequenceRowsQueryByExternalId(
        externalId,
        inclusiveStart,
        exclusiveEnd,
        limit,
        None,
        columns
      ),
      batchSize
    )
}

object SequenceRows {
  implicit val cogniteIdEncoder: Encoder[CogniteInternalId] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val sequenceColumnIdDecoder: Decoder[SequenceColumnSignature] = deriveDecoder
  implicit val sequenceRowEncoder: Encoder[SequenceRow] = deriveEncoder
  implicit val sequenceRowDecoder: Decoder[SequenceRow] = deriveDecoder
  implicit val sequenceRowsInsertByIdEncoder: Encoder[SequenceRowsInsertById] = deriveEncoder
  implicit val sequenceRowsInsertByIdItemsEncoder: Encoder[Items[SequenceRowsInsertById]] =
    deriveEncoder
  implicit val sequenceRowsInsertByExternalIdEncoder: Encoder[SequenceRowsInsertByExternalId] =
    deriveEncoder
  implicit val sequenceRowsInsertByExternalIdItemsEncoder
      : Encoder[Items[SequenceRowsInsertByExternalId]] = deriveEncoder
  implicit val sequenceRowsDeleteByIdEncoder: Encoder[SequenceRowsDeleteById] = deriveEncoder
  implicit val sequenceRowsDeleteByIdItemsEncoder: Encoder[Items[SequenceRowsDeleteById]] =
    deriveEncoder
  implicit val sequenceRowsDeleteByExternalIdEncoder: Encoder[SequenceRowsDeleteByExternalId] =
    deriveEncoder
  implicit val sequenceRowsDeleteByExternalIdItemsEncoder
      : Encoder[Items[SequenceRowsDeleteByExternalId]] = deriveEncoder

  implicit val sequenceRowsQueryByIdEncoder: Encoder[SequenceRowsQueryById] = deriveEncoder
  implicit val sequenceRowsQueryByIdItemsEncoder: Encoder[Items[SequenceRowsQueryById]] =
    deriveEncoder
  implicit val sequenceRowsQueryByExternalIdEncoder: Encoder[SequenceRowsQueryByExternalId] =
    deriveEncoder
  implicit val sequenceRowsQueryByExternalIdItemsEncoder
      : Encoder[Items[SequenceRowsQueryByExternalId]] = deriveEncoder
  @SuppressWarnings(
    Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable")
  )
  implicit val sequenceRowsResponseDecoder: Decoder[SequenceRowsResponse] = deriveDecoder

  implicit val sequenceRowsQueryEncoder: Encoder[SequenceRowsQuery] = {
    case q: SequenceRowsQueryById => sequenceRowsQueryByIdEncoder(q)
    case q: SequenceRowsQueryByExternalId => sequenceRowsQueryByExternalIdEncoder(q)
  }
}
