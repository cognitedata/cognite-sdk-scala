package com.cognite.sdk.scala.v1.resources

import java.time.Instant

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

import java.io.FileInputStream

class Files[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[File, F]
    with RetrieveByIds[File, F]
    with Create[File, FileCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Filter[File, FilesFilter, F]
    with Search[File, FilesQuery, F]
    with Update[File, FileUpdate, F] {
  import Files._
  override val baseUri = uri"${requestSession.baseUri}/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError, File]] =
    EitherDecoder.eitherDecoder[CdpApiError, File]

  override def createItems(items: Items[FileCreate]): F[Seq[File]] =
    items.items match {
      case item :: Nil => {
        requestSession
          .sendCdf { request =>
            request
              .post(baseUri)
              .body(item)
              .response(asJson[Either[CdpApiError, File]])
              .mapResponse {
                case Left(value) => throw value.error
                case Right(Left(cdpApiError)) =>
                  throw cdpApiError.asException(uri"$baseUri/byids")
                case Right(Right(value)) => Seq(value)
              }
          }
      }
      case _ => throw new RuntimeException("Files only support creating one file per call")
    }

  def uploadWithName(file: java.io.File, name: String): F[File] = {
    val inputStream = new FileInputStream(file)
    val item = FileCreate(name = name)
    requestSession.flatMap(
      createOne(item),
      (file: File) =>
        file.uploadUrl match {
          case Some(uploadUrl) =>
            val response = requestSession.send { request =>
              request
                .body(inputStream)
                .put(uri"$uploadUrl")
            }
            requestSession.map(
              response,
              (res: Response[String]) =>
                if (res.isSuccess) {
                  file
                } else {
                  throw SdkException(
                    s"File upload of file ${file.name} failed with error code ${res.code.toString()}"
                  )
                }
            )
          case None =>
            throw SdkException(s"File upload of file ${file.name} did not return uploadUrl")
        }
    )
  }

  def upload(file: java.io.File): F[File] =
    uploadWithName(file, file.getName())

  override def readWithCursor(
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[File]] =
    Readable.readWithCursor(requestSession, baseUri, cursor, limit)

  override def retrieveByIds(ids: Seq[Long]): F[Seq[File]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def update(items: Seq[FileUpdate]): F[Seq[File]] =
    Update.update[F, File, FileUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def filterWithCursor(
      filter: FilesFilter,
      cursor: Option[String],
      limit: Option[Long]
  ): F[ItemsWithCursor[File]] =
    Filter.filterWithCursor(requestSession, baseUri, filter, cursor, limit)

  override def search(searchQuery: FilesQuery): F[Seq[File]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object Files {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val fileItemsWithCursorDecoder: Decoder[ItemsWithCursor[File]] =
    deriveDecoder[ItemsWithCursor[File]]
  implicit val fileDecoder: Decoder[File] = deriveDecoder[File]
  implicit val fileItemsDecoder: Decoder[Items[File]] =
    deriveDecoder[Items[File]]
  implicit val createFileEncoder: Encoder[FileCreate] =
    deriveEncoder[FileCreate]
  implicit val createFileItemsEncoder: Encoder[Items[FileCreate]] =
    deriveEncoder[Items[FileCreate]]
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
