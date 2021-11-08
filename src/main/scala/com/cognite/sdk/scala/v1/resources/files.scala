// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import java.io.{BufferedInputStream, FileInputStream}

import cats.implicits._
import cats.Applicative
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

class Files[F[_]: Applicative](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with PartitionedReadable[File, F]
    with RetrieveByIdsWithIgnoreUnknownIds[File, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[File, F]
    with Create[File, FileCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with PartitionedFilter[File, FilesFilter, F]
    with Search[File, FilesQuery, F]
    with UpdateById[File, FileUpdate, F]
    with UpdateByExternalId[File, FileUpdate, F] {
  import Files._
  override val baseUrl = uri"${requestSession.baseUrl}/files"

  implicit val errorOrFileDecoder: Decoder[Either[CdpApiError, File]] =
    EitherDecoder.eitherDecoder[CdpApiError, File]

  override def createOne(item: FileCreate): F[File] =
    requestSession
      .post[File, File, FileCreate](
        item,
        baseUrl,
        value => value
      )

  override def createItems(items: Items[FileCreate]): F[Seq[File]] =
    items.items.toList.traverse(createOne).map(_.toSeq)

  def uploadWithName(input: java.io.InputStream, name: String): F[File] = {
    val item = FileCreate(name = name)
    requestSession.flatMap(
      createOne(item),
      (file: File) =>
        file.uploadUrl match {
          case Some(uploadUrl) =>
            val response = requestSession.send { request =>
              request
                .body(input)
                .put(uri"$uploadUrl")
            }
            requestSession.map(
              response,
              (res: Response[Either[String, String]]) =>
                if (res.isSuccess) {
                  file
                } else {
                  throw SdkException(
                    s"File upload of file ${file.name} failed with error code ${res.code.toString}"
                  )
                }
            )
          case None =>
            throw SdkException(s"File upload of file ${file.name} did not return uploadUrl")
        }
    )
  }

  def upload(file: java.io.File): F[File] = {
    val inputStream = new BufferedInputStream(new FileInputStream(file))
    uploadWithName(inputStream, file.getName)
  }

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[File]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long], ignoreUnknownIds: Boolean): F[Seq[File]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[File]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def updateById(items: Map[Long, FileUpdate]): F[Seq[File]] =
    UpdateById.updateById[F, File, FileUpdate](requestSession, baseUrl, items)

  override def updateByExternalId(items: Map[String, FileUpdate]): F[Seq[File]] =
    UpdateByExternalId.updateByExternalId[F, File, FileUpdate](requestSession, baseUrl, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUrl, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  private[sdk] def filterWithCursor(
      filter: FilesFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[File]] =
    Filter.filterWithCursor(
      requestSession,
      baseUrl,
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize
    )

  override def search(searchQuery: FilesQuery): F[Seq[File]] =
    Search.search(requestSession, baseUrl, searchQuery)

  def download(item: FileDownload, out: java.io.OutputStream): F[Unit] = {
    val request =
      requestSession
        .post[Items[FileDownloadLink], Items[FileDownloadLink], Items[FileDownload]](
          Items(Seq(item)),
          uri"${baseUrl.toString}/downloadlink",
          values => values
        )

    requestSession.flatMap(
      request,
      (files: Items[FileDownloadLink]) => {
        val response = requestSession.send { request =>
          request
            .get(
              uri"${files.items
                .map(_.downloadUrl)
                .headOption
                .getOrElse(throw SdkException(s"File download of ${item.toString} did not return download url"))}"
            )
            .response(asByteArray)
        }
        requestSession.map(
          response,
          (res: Response[Either[String, Array[Byte]]]) =>
            res.body match {
              case Right(bytes) => out.write(bytes)
              case Left(_) =>
                throw SdkException(
                  s"File download of file ${item.toString} failed with error code ${res.code.toString}"
                )
            }
        )
      }
    )
  }
}

object Files {
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
  implicit val fileDownloadLinkIdDecoder: Decoder[FileDownloadLinkId] =
    deriveDecoder[FileDownloadLinkId]
  implicit val fileDownloadLinkExternalIdDecoder: Decoder[FileDownloadLinkExternalId] =
    deriveDecoder[FileDownloadLinkExternalId]
  implicit val fileDownloadIdEncoder: Encoder[FileDownloadId] =
    deriveEncoder[FileDownloadId]
  implicit val fileDownloadExternalIdEncoder: Encoder[FileDownloadExternalId] =
    deriveEncoder[FileDownloadExternalId]
  implicit val fileDownloadEncoder: Encoder[FileDownload] = Encoder.instance {
    case downloadId @ FileDownloadId(_) => fileDownloadIdEncoder(downloadId)
    case downloadExternalId @ FileDownloadExternalId(_) =>
      fileDownloadExternalIdEncoder(downloadExternalId)
  }
  implicit val fileDownloadLinkDecoder: Decoder[FileDownloadLink] =
    fileDownloadLinkIdDecoder.widen.or(fileDownloadLinkExternalIdDecoder.widen)
  implicit val fileDownloadItemsEncoder: Encoder[Items[FileDownload]] =
    deriveEncoder[Items[FileDownload]]
  implicit val fileDownloadItemsDecoder: Decoder[Items[FileDownloadLink]] =
    deriveDecoder[Items[FileDownloadLink]]
  implicit val fileDownloadResponseDecoder: Decoder[Either[CdpApiError, Items[FileDownloadLink]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[FileDownloadLink]]
}
