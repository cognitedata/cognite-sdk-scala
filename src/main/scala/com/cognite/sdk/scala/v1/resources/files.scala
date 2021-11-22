// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import java.io.{BufferedInputStream, FileInputStream}
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._
import sttp.monad.MonadError
import sttp.monad.syntax._

class Files[F[_]: MonadError](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with RetrieveByIdsWithIgnoreUnknownIds[File, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[File, F]
    with Create[File, FileCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Search[File, FilesQuery, F]
    with UpdateById[File, FileUpdate, F]
    with UpdateByExternalId[File, FileUpdate, F] {
  import Files._
  override val baseUrl = uri"${requestSession.baseUrl}/files"

  implicit val errorOrFileCodec: JsonValueCodec[Either[CdpApiError, File]] =
    JsonCodecMaker.make[Either[CdpApiError, File]]

  override def createOne(item: FileCreate): F[File] =
    requestSession
      .post[File, File, FileCreate](
        item,
        baseUrl,
        value => value
      )

  override def createItems(items: Items[FileCreate]): F[Seq[File]] =
    items.items.foldRight(List.empty[File].unit) { (item, acc: F[List[File]]) =>
      createOne(item).flatMap { file =>
        acc.map(files => file :: files)
      }
    }.map(_.toSeq)

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
  implicit val fileItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[File]] =
    JsonCodecMaker.make[ItemsWithCursor[File]]
  implicit val fileCodec: JsonValueCodec[File] = JsonCodecMaker.make[File]
  implicit val fileItemsCodec: JsonValueCodec[Items[File]] =
    JsonCodecMaker.make[Items[File]]
  implicit val createFileCodec: JsonValueCodec[FileCreate] =
    JsonCodecMaker.make[FileCreate]
  implicit val createFileItemsCodec: JsonValueCodec[Items[FileCreate]] =
    JsonCodecMaker.make[Items[FileCreate]]
  implicit val fileUpdateCodec: JsonValueCodec[FileUpdate] =
    JsonCodecMaker.make[FileUpdate]
  implicit val updateFilesItemsCodec: JsonValueCodec[Items[FileUpdate]] =
    JsonCodecMaker.make[Items[FileUpdate]]
  implicit val filesFilterCodec: JsonValueCodec[FilesFilter] =
    JsonCodecMaker.make[FilesFilter]
  implicit val filesSearchCodec: JsonValueCodec[FilesSearch] =
    JsonCodecMaker.make[FilesSearch]
  implicit val filesQueryCodec: JsonValueCodec[FilesQuery] =
    JsonCodecMaker.make[FilesQuery]
  implicit val filesFilterRequestCodec: JsonValueCodec[FilterRequest[FilesFilter]] =
    JsonCodecMaker.make[FilterRequest[FilesFilter]]
  implicit val fileDownloadLinkIdCodec: JsonValueCodec[FileDownloadLinkId] =
    JsonCodecMaker.make[FileDownloadLinkId]
  implicit val fileDownloadLinkExternalIdCodec: JsonValueCodec[FileDownloadLinkExternalId] =
    JsonCodecMaker.make[FileDownloadLinkExternalId]
  implicit val fileDownloadIdCodec: JsonValueCodec[FileDownloadId] =
    JsonCodecMaker.make[FileDownloadId]
  implicit val fileDownloadExternalIdCodec: JsonValueCodec[FileDownloadExternalId] =
    JsonCodecMaker.make[FileDownloadExternalId]
  implicit val fileDownloadCodec: JsonValueCodec[FileDownload] = Codec.instance {
    case downloadId @ FileDownloadId(_) => fileDownloadIdCodec(downloadId)
    case downloadExternalId @ FileDownloadExternalId(_) =>
      fileDownloadExternalIdCodec(downloadExternalId)
  }
  implicit val fileDownloadLinkCodec: JsonValueCodec[FileDownloadLink] =
    fileDownloadLinkIdCodec.widen.or(fileDownloadLinkExternalIdCodec.widen)
  implicit val fileDownloadItemsCodec: JsonValueCodec[Items[FileDownload]] =
    JsonCodecMaker.make[Items[FileDownload]]
  implicit val fileDownloadItemsCodec: JsonValueCodec[Items[FileDownloadLink]] =
    JsonCodecMaker.make[Items[FileDownloadLink]]
  implicit val fileDownloadResponseCodec: JsonValueCodec[Either[CdpApiError, Items[FileDownloadLink]]] =
    JsonCodecMaker.make[Either[CdpApiError, Items[FileDownloadLink]]]
}
