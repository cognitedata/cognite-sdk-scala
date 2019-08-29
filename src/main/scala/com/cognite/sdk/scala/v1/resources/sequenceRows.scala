package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

class sequenceData {}

class SequenceRows[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUri {
  import SequenceRows._
  override val baseUri = uri"${requestSession.baseUri}/sequences/data"

  implicit val errorOrItemsSequenceRowsResponseDecoder
      : Decoder[Either[CdpApiError, Items[SequenceRowsResponse]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[SequenceRowsResponse]]
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def insertById(id: Long, columns: Seq[Long], rows: Seq[SequenceRow]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(Items(Seq(SequenceRowsInsertById(id, columns.map(CogniteId), rows))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def insertByExternalId(
      externalId: String,
      columns: Seq[String],
      rows: Seq[SequenceRow]
  ): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(
            Items(
              Seq(SequenceRowsInsertByExternalId(externalId, columns.map(CogniteExternalId), rows))
            )
          )
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(_)) => ()
          }
      }

  def deleteById(id: Long, rows: Seq[Long]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(Seq(SequenceRowsDeleteById(id, rows))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

  def deleteByExternalId(externalId: String, rows: Seq[Long]): F[Unit] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(Seq(SequenceRowsDeleteByExternalId(externalId, rows))))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

  def queryById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByIdWithLimitAndMaybeColumns(id, inclusiveStart, exclusiveEnd)

  def queryByIdWithLimit(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      limit: Int
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByIdWithLimitAndMaybeColumns(id, inclusiveStart, exclusiveEnd, limit = limit)

  def queryByIdWithColumns(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      columns: Seq[Long]
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByIdWithLimitAndMaybeColumns(id, inclusiveStart, exclusiveEnd, columns = Some(columns))

  def queryByIdWithColumnsAndLimit(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      columns: Seq[Long],
      limit: Int
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByIdWithLimitAndMaybeColumns(id, inclusiveStart, exclusiveEnd, limit, Some(columns))

  private def queryByIdWithLimitAndMaybeColumns(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      limit: Int = 100,
      columns: Option[Seq[Long]] = None
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/list")
          .body(Items(Seq(SequenceRowsQueryById(id, inclusiveStart, exclusiveEnd, limit, columns))))
          .response(asJson[Either[CdpApiError, Items[SequenceRowsResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => (items.columns.toList, items.rows)
                case None => (Seq.empty, Seq.empty)
              }
          }
      }

  def queryByExternalId(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByExternalIdWithLimitAndMaybeColumns(externalId, inclusiveStart, exclusiveEnd)

  def queryByExternalIdWithLimit(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      limit: Int
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByExternalIdWithLimitAndMaybeColumns(
      externalId,
      inclusiveStart,
      exclusiveEnd,
      limit = limit
    )

  def queryByExternalIdWithColumns(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      columns: Seq[String]
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByExternalIdWithLimitAndMaybeColumns(
      externalId,
      inclusiveStart,
      exclusiveEnd,
      columns = Some(columns)
    )

  def queryByExternalIdWithColumnsAndLimit(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      columns: Seq[String],
      limit: Int
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    queryByExternalIdWithLimitAndMaybeColumns(
      externalId,
      inclusiveStart,
      exclusiveEnd,
      limit,
      Some(columns)
    )

  private def queryByExternalIdWithLimitAndMaybeColumns(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      limit: Int = 100,
      columns: Option[Seq[String]] = None
  ): F[(Seq[SequenceColumnId], Seq[SequenceRow])] =
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/list")
          .body(
            Items(
              Seq(
                SequenceRowsQueryByExternalId(
                  externalId,
                  inclusiveStart,
                  exclusiveEnd,
                  limit,
                  columns
                )
              )
            )
          )
          .response(asJson[Either[CdpApiError, Items[SequenceRowsResponse]]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/list")
            case Right(Right(value)) =>
              value.items.headOption match {
                case Some(items) => (items.columns.toList, items.rows)
                case None => (Seq.empty, Seq.empty)
              }
          }
      }
}

object SequenceRows {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val sequenceColumnIdDecoder: Decoder[SequenceColumnId] = deriveDecoder
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
  implicit val sequenceRowsResponseItemsDecoder: Decoder[Items[SequenceRowsResponse]] =
    deriveDecoder
}
