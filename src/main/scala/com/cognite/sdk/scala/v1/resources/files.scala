package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

class Files[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[File, F]
    with RetrieveByIds[File, F]
    with Create[File, CreateFile, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Filter[File, FilesFilter, F]
    with Search[File, FilesQuery, F]
    with Update[File, FileUpdate, F] {
  import Files._
  override val baseUri = uri"${requestSession.baseUri}/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError, File]] =
    EitherDecoder.eitherDecoder[CdpApiError, File]
  override def createItems(items: Items[CreateFile]): F[Response[Seq[File]]] =
    items.items match {
      case item :: Nil =>
        requestSession
          .send { request =>
            request
              .post(baseUri)
              .body(item)
              .response(asJson[Either[CdpApiError, File]])
              .mapResponse {
                case Left(value) => throw value.error
                case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
                case Right(Right(value)) => Seq(value)
              }
          }
      case _ => throw new RuntimeException("Files only support creating one file per call")
    }

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[File]]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Response[Seq[File]]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def updateItems(items: Seq[FileUpdate]): F[Response[Seq[File]]] =
    Update.updateItems[F, File, FileUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Response[Unit]] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def filterWithCursor(
      filter: FilesFilter,
      cursor: Option[String],
      limit: Option[Long]
  ): F[Response[ItemsWithCursor[File]]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit)

  override def search(searchQuery: FilesQuery): F[Response[Seq[File]]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object Files {
  implicit val fileItemsWithCursorDecoder: Decoder[ItemsWithCursor[File]] =
    deriveDecoder[ItemsWithCursor[File]]
  implicit val fileDecoder: Decoder[File] = deriveDecoder[File]
  implicit val fileItemsDecoder: Decoder[Items[File]] =
    deriveDecoder[Items[File]]
  implicit val createFileEncoder: Encoder[CreateFile] =
    deriveEncoder[CreateFile]
  implicit val createFileItemsEncoder: Encoder[Items[CreateFile]] =
    deriveEncoder[Items[CreateFile]]
  implicit val fileUpdateEncoder: Encoder[FileUpdate] =
    deriveEncoder[FileUpdate]
  implicit val updateFilesItemsEncoder: Encoder[Items[FileUpdate]] =
    deriveEncoder[Items[FileUpdate]]
  implicit val filesFilterEncoder: Encoder[FilesFilter] =
    deriveEncoder[FilesFilter]
  implicit val filesSearchEncoder: Encoder[FilesSearch] =
    deriveEncoder[FilesSearch]
  implicit val filesQueryEncoder: Encoder[FilesQuery] =
    deriveEncoder[FilesQuery]
  implicit val filesFilterRequestEncoder: Encoder[FilterRequest[FilesFilter]] =
    deriveEncoder[FilterRequest[FilesFilter]]
}
