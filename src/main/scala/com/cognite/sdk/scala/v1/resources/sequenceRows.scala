// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._

class SequenceRows[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import SequenceRows._
  override val baseUrl = uri"${requestSession.baseUrl}/sequences/data"

  implicit val errorOrItemsSequenceRowsResponseCodec
      : JsonValueCodec[Either[CdpApiError, SequenceRowsResponse]] =
    JsonCodecMaker.make[Either[CdpApiError, SequenceRowsResponse]]
  implicit val errorOrUnitCodec: JsonValueCodec[Either[CdpApiError, Unit]] =
    JsonCodecMaker.make[Either[CdpApiError, Unit]]

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
}

object SequenceRows {
  implicit val cogniteIdCodec: JsonValueCodec[CogniteInternalId] = JsonCodecMaker.make
  implicit val cogniteExternalIdCodec: JsonValueCodec[CogniteExternalId] = JsonCodecMaker.make
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val sequenceColumnIdCodec: JsonValueCodec[SequenceColumnSignature] = JsonCodecMaker.make
  implicit val sequenceRowCodec: JsonValueCodec[SequenceRow] = JsonCodecMaker.make
  implicit val sequenceRowsInsertByIdCodec: JsonValueCodec[SequenceRowsInsertById] = JsonCodecMaker.make
  implicit val sequenceRowsInsertByIdItemsCodec: JsonValueCodec[Items[SequenceRowsInsertById]] =
    JsonCodecMaker.make
  implicit val sequenceRowsInsertByExternalIdCodec: JsonValueCodec[SequenceRowsInsertByExternalId] =
    JsonCodecMaker.make
  implicit val sequenceRowsInsertByExternalIdItemsCodec
      : JsonValueCodec[Items[SequenceRowsInsertByExternalId]] = JsonCodecMaker.make
  implicit val sequenceRowsDeleteByIdCodec: JsonValueCodec[SequenceRowsDeleteById] = JsonCodecMaker.make
  implicit val sequenceRowsDeleteByIdItemsCodec: JsonValueCodec[Items[SequenceRowsDeleteById]] =
    JsonCodecMaker.make
  implicit val sequenceRowsDeleteByExternalIdCodec: JsonValueCodec[SequenceRowsDeleteByExternalId] =
    JsonCodecMaker.make
  implicit val sequenceRowsDeleteByExternalIdItemsCodec
      : JsonValueCodec[Items[SequenceRowsDeleteByExternalId]] = JsonCodecMaker.make

  implicit val sequenceRowsQueryByIdCodec: JsonValueCodec[SequenceRowsQueryById] = JsonCodecMaker.make
  implicit val sequenceRowsQueryByIdItemsCodec: JsonValueCodec[Items[SequenceRowsQueryById]] =
    JsonCodecMaker.make
  implicit val sequenceRowsQueryByExternalIdCodec: JsonValueCodec[SequenceRowsQueryByExternalId] =
    JsonCodecMaker.make
  implicit val sequenceRowsQueryByExternalIdItemsCodec
      : JsonValueCodec[Items[SequenceRowsQueryByExternalId]] = JsonCodecMaker.make
  @SuppressWarnings(
    Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable")
  )
  implicit val sequenceRowsResponseCodec: JsonValueCodec[SequenceRowsResponse] = JsonCodecMaker.make

  implicit val sequenceRowsQueryCodec: JsonValueCodec[SequenceRowsQuery] = {
    case _: SequenceRowsQueryById => sequenceRowsQueryByIdCodec
    case _: SequenceRowsQueryByExternalId => sequenceRowsQueryByExternalIdCodec
  }
}
