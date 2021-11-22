// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._
import sttp.client3.jsoniter_scala._
import sttp.model.Uri

class Functions[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[Function, FunctionCreate, F]
    with RetrieveByIds[Function, F]
    with RetrieveByExternalIds[Function, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F] {
  import Functions._
  override val baseUrl = uri"${requestSession.baseUrl}/functions"

  def read(): F[Items[Function]] =
    requestSession.get[Items[Function], Items[Function]](
      baseUrl,
      value => value
    )

  override def createItems(items: Items[FunctionCreate]): F[Seq[Function]] = {
    //implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[Function], Items[Function], Items[FunctionCreate]](
      items,
      baseUrl,
      value => value.items
    )
  }

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUrl, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  override def retrieveByIds(ids: Seq[Long]): F[Seq[Function]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUrl, ids)

  override def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[Function]] =
    RetrieveByExternalIds.retrieveByExternalIds(requestSession, baseUrl, externalIds)
}

object Functions {
  implicit val functionCodec: JsonValueCodec[Function] = JsonCodecMaker.make[Function]
  implicit val functionsItemsCodec: JsonValueCodec[Items[Function]] =
    JsonCodecMaker.make[Items[Function]]
  implicit val functionsItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Function]] =
    JsonCodecMaker.make[ItemsWithCursor[Function]]
  implicit val functionErrorCodec: JsonValueCodec[FunctionError] = JsonCodecMaker.make[FunctionError]
  implicit val createFunctionCodec: JsonValueCodec[FunctionCreate] = JsonCodecMaker.make[FunctionCreate]
  implicit val createFunctionsItemsCodec: JsonValueCodec[Items[FunctionCreate]] =
    JsonCodecMaker.make[Items[FunctionCreate]]

  implicit val errorOrFunctionCodec: JsonValueCodec[Either[CdpApiError, Items[Function]]] =
    JsonCodecMaker.make[Either[CdpApiError, Items[Function]]]
  implicit val errorOrUnitCodec: JsonValueCodec[Either[CdpApiError, Unit]] =
    JsonCodecMaker.make[Either[CdpApiError, Unit]]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsCodec
      : JsonValueCodec[ItemsWithRecursiveAndIgnoreUnknownIds] =
    JsonCodecMaker.make[ItemsWithRecursiveAndIgnoreUnknownIds]
}

class FunctionCalls[F[_]](val requestSession: RequestSession[F], val functionId: Long)
    extends WithRequestSession[F]
    with BaseUrl {
  import FunctionCalls._
  override val baseUrl = uri"${requestSession.baseUrl}/functions/$functionId/calls"

  def callFunction(data: Json): F[FunctionCall] =
    requestSession.post[FunctionCall, FunctionCall, Json](
      data,
      uri"${baseUrl.toString.dropRight(1)}",
      value => value
    )

  def read(): F[Items[FunctionCall]] =
    requestSession.get[Items[FunctionCall], Items[FunctionCall]](
      baseUrl,
      value => value
    )

  def filter(filter: FunctionCallFilter): F[Items[FunctionCall]] = {
    //implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession
      .post[Items[FunctionCall], Items[FunctionCall], FilterRequest[FunctionCallFilter]](
        FilterRequest(filter, None, None, None, None),
        uri"${baseUrl}/list",
        value => value
      )
  }

  def retrieveById(id: Long): F[FunctionCall] =
    requestSession.get[FunctionCall, FunctionCall](
      uri"$baseUrl/$id",
      value => value
    )

  def retrieveLogs(callId: Long): F[Items[FunctionCallLogEntry]] =
    requestSession.get[Items[FunctionCallLogEntry], Items[FunctionCallLogEntry]](
      uri"$baseUrl/$callId/logs",
      value => value
    )

  def retrieveResponse(callId: Long): F[FunctionCallResponse] =
    requestSession.get[FunctionCallResponse, FunctionCallResponse](
      uri"$baseUrl/$callId/response",
      value => value
    )
}

object FunctionCalls {
  implicit val functionCallCodec: JsonValueCodec[FunctionCall] = JsonCodecMaker.make[FunctionCall]
  implicit val functionCallLogEntryCodec: JsonValueCodec[FunctionCallLogEntry] =
    JsonCodecMaker.make[FunctionCallLogEntry]
  implicit val functionCallResponseCodec: JsonValueCodec[FunctionCallResponse] =
    JsonCodecMaker.make[FunctionCallResponse]
  implicit val functionCallItemsCodec: JsonValueCodec[Items[FunctionCall]] =
    JsonCodecMaker.make[Items[FunctionCall]]
  implicit val functionCallLogEntryItemsCodec: JsonValueCodec[Items[FunctionCallLogEntry]] =
    JsonCodecMaker.make[Items[FunctionCallLogEntry]]

  implicit val FunctionCallFilterCodec: JsonValueCodec[FunctionCallFilter] =
    JsonCodecMaker.make[FunctionCallFilter]
  implicit val FunctionCallFilterRequestCodec: JsonValueCodec[FilterRequest[FunctionCallFilter]] =
    JsonCodecMaker.make[FilterRequest[FunctionCallFilter]]

  implicit val errorOrUnitCodec: JsonValueCodec[Either[CdpApiError, Unit]] =
    JsonCodecMaker.make[Either[CdpApiError, Unit]]
  implicit val errorOrItemsCodec: JsonValueCodec[Either[CdpApiError, Items[FunctionCall]]] =
    JsonCodecMaker.make
  implicit val errorOrFunctionCallCodec: JsonValueCodec[Either[CdpApiError, FunctionCall]] =
    JsonCodecMaker.make[Either[CdpApiError, FunctionCall]]
  implicit val errorOrLogEntriesCodec: JsonValueCodec[Either[CdpApiError, Items[FunctionCallLogEntry]]] =
    JsonCodecMaker.make
  implicit val errorOrFunctionResponseCodec: JsonValueCodec[Either[CdpApiError, FunctionCallResponse]] =
    JsonCodecMaker.make[Either[CdpApiError, FunctionCallResponse]]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsCodec
      : JsonValueCodec[ItemsWithRecursiveAndIgnoreUnknownIds] =
    JsonCodecMaker.make[ItemsWithRecursiveAndIgnoreUnknownIds]
}

class FunctionSchedules[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[FunctionSchedule, FunctionScheduleCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F] {
  import FunctionSchedules._
  override val baseUrl: Uri = uri"${requestSession.baseUrl}/functions/schedules"

  def read(): F[Items[FunctionSchedule]] =
    requestSession.get[Items[FunctionSchedule], Items[FunctionSchedule]](
      baseUrl,
      value => value
    )

  override def createItems(items: Items[FunctionScheduleCreate]): F[Seq[FunctionSchedule]] =
    Create.createItems[F, FunctionSchedule, FunctionScheduleCreate](requestSession, baseUrl, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUrl, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)
}

object FunctionSchedules {
  implicit val functionScheduleCodec: JsonValueCodec[FunctionSchedule] = JsonCodecMaker.make[FunctionSchedule]
  implicit val createFunctionScheduleCodec: JsonValueCodec[FunctionScheduleCreate] =
    JsonCodecMaker.make[FunctionScheduleCreate]
  implicit val createFunctionScheduleItemsCodec: JsonValueCodec[Items[FunctionScheduleCreate]] =
    JsonCodecMaker.make[Items[FunctionScheduleCreate]]
  implicit val functionScheduleItemsCodec: JsonValueCodec[Items[FunctionSchedule]] =
    JsonCodecMaker.make[Items[FunctionSchedule]]
  implicit val functionScheduleItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[FunctionSchedule]] =
    JsonCodecMaker.make[ItemsWithCursor[FunctionSchedule]]

  implicit val errorOrUnitCodec: JsonValueCodec[Either[CdpApiError, Unit]] =
    JsonCodecMaker.make[Either[CdpApiError, Unit]]
  implicit val errorOrItemsCodec: JsonValueCodec[Either[CdpApiError, Items[FunctionSchedule]]] =
    JsonCodecMaker.make
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsCodec
      : JsonValueCodec[ItemsWithRecursiveAndIgnoreUnknownIds] =
    JsonCodecMaker.make[ItemsWithRecursiveAndIgnoreUnknownIds]
}
