// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.Json
import io.circe.Printer
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
    implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
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
  implicit val functionErrorDecoder: Decoder[FunctionError] = deriveDecoder[FunctionError]
  implicit val functionDecoder: Decoder[Function] = deriveDecoder[Function]
  implicit val functionsItemsDecoder: Decoder[Items[Function]] =
    deriveDecoder[Items[Function]]
  implicit val functionsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Function]] =
    deriveDecoder[ItemsWithCursor[Function]]
  implicit val functionErrorEncoder: Encoder[FunctionError] = deriveEncoder[FunctionError]
  implicit val createFunctionEncoder: Encoder[FunctionCreate] = deriveEncoder[FunctionCreate]
  implicit val createFunctionsItemsEncoder: Encoder[Items[FunctionCreate]] =
    deriveEncoder[Items[FunctionCreate]]

  implicit val errorOrFunctionDecoder: Decoder[Either[CdpApiError, Items[Function]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[Function]]
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
}

class FunctionCalls[F[_]](val requestSession: RequestSession[F], val functionId: Long)
    extends WithRequestSession[F]
    with BaseUrl {
  import FunctionCalls._
  override val baseUrl = uri"${requestSession.baseUrl}/functions/$functionId/calls"

  def callFunction(data: Json): F[FunctionCall] =
    requestSession.post[FunctionCall, FunctionCall, Json](
      data,
      uri"${baseUrl.toString().dropRight(1)}",
      value => value
    )

  def read(): F[Items[FunctionCall]] =
    requestSession.get[Items[FunctionCall], Items[FunctionCall]](
      baseUrl,
      value => value
    )

  def filter(filter: FunctionCallFilter): F[Items[FunctionCall]] = {
    implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
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
  implicit val functionCallDecoder: Decoder[FunctionCall] = deriveDecoder[FunctionCall]
  implicit val functionCallLogEntryDecoder: Decoder[FunctionCallLogEntry] =
    deriveDecoder[FunctionCallLogEntry]
  implicit val functionCallResponseDecoder: Decoder[FunctionCallResponse] =
    deriveDecoder[FunctionCallResponse]
  implicit val functionCallItemsDecoder: Decoder[Items[FunctionCall]] =
    deriveDecoder[Items[FunctionCall]]
  implicit val functionCallLogEntryItemsDecoder: Decoder[Items[FunctionCallLogEntry]] =
    deriveDecoder[Items[FunctionCallLogEntry]]

  implicit val FunctionCallFilterEncoder: Encoder[FunctionCallFilter] =
    deriveEncoder[FunctionCallFilter]
  implicit val FunctionCallFilterRequestEncoder: Encoder[FilterRequest[FunctionCallFilter]] =
    deriveEncoder[FilterRequest[FunctionCallFilter]]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[FunctionCall]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[FunctionCall]]
  implicit val errorOrFunctionCallDecoder: Decoder[Either[CdpApiError, FunctionCall]] =
    EitherDecoder.eitherDecoder[CdpApiError, FunctionCall]
  implicit val errorOrLogEntriesDecoder: Decoder[Either[CdpApiError, Items[FunctionCallLogEntry]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[FunctionCallLogEntry]]
  implicit val errorOrFunctionResponseDecoder: Decoder[Either[CdpApiError, FunctionCallResponse]] =
    EitherDecoder.eitherDecoder[CdpApiError, FunctionCallResponse]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
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
  implicit val functionScheduleDecoder: Decoder[FunctionSchedule] = deriveDecoder[FunctionSchedule]
  implicit val createFunctionScheduleEncoder: Encoder[FunctionScheduleCreate] =
    deriveEncoder[FunctionScheduleCreate]
  implicit val createFunctionScheduleItemsEncoder: Encoder[Items[FunctionScheduleCreate]] =
    deriveEncoder[Items[FunctionScheduleCreate]]
  implicit val functionScheduleItemsDecoder: Decoder[Items[FunctionSchedule]] =
    deriveDecoder[Items[FunctionSchedule]]
  implicit val functionScheduleItemsWithCursorDecoder: Decoder[ItemsWithCursor[FunctionSchedule]] =
    deriveDecoder[ItemsWithCursor[FunctionSchedule]]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[FunctionSchedule]]] =
    EitherDecoder.eitherDecoder[CdpApiError, Items[FunctionSchedule]]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
}
