// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.client3._
import sttp.client3.circe._

class DataModelInstances[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  import DataModelInstances._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/instances"

  def createItems(items: Items[DataModelInstance]): F[Seq[DataModelInstance]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[DataModelInstance], Items[DataModelInstance], Items[
      DataModelInstance
    ]](
      items,
      uri"$baseUrl/ingest",
      value => value.items
    )
  }

  def query(
      inputQuery: DataModelInstanceQuery
  ): F[ItemsWithCursor[DataModelInstanceQueryResponse]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[ItemsWithCursor[DataModelInstanceQueryResponse], ItemsWithCursor[
      DataModelInstanceQueryResponse
    ], DataModelInstanceQuery](
      inputQuery,
      uri"$baseUrl/list",
      value => value
    )
  }
}

object DataModelInstances {
  implicit val dataModelInstanceEncoder: Encoder[DataModelInstance] =
    deriveEncoder[DataModelInstance]
  implicit val dataModelInstanceItemsEncoder: Encoder[Items[DataModelInstance]] =
    deriveEncoder[Items[DataModelInstance]]
  implicit val dataModelInstanceDecoder: Decoder[DataModelInstance] =
    deriveDecoder[DataModelInstance]
  implicit val dataModelInstanceItemsDecoder: Decoder[Items[DataModelInstance]] =
    deriveDecoder[Items[DataModelInstance]]

  implicit val dmiAndFilterEncoder: Encoder[DMIAndFilter] = deriveEncoder[DMIAndFilter]
  implicit val dmiOrFilterEncoder: Encoder[DMIOrFilter] = deriveEncoder[DMIOrFilter]
  implicit val dmiNotFilterEncoder: Encoder[DMINotFilter] = deriveEncoder[DMINotFilter]

  implicit val dmiEqualsFilterEncoder: Encoder[DMIEqualsFilter] = deriveEncoder[DMIEqualsFilter]
  implicit val dmiInFilterEncoder: Encoder[DMIInFilter] = deriveEncoder[DMIInFilter]
  implicit val dmiRangeFilterEncoder: Encoder[DMIRangeFilter] =
    deriveEncoder[DMIRangeFilter].mapJson(_.dropNullValues) // VH TODO make this common

  implicit val dmiPrefixFilterEncoder: Encoder[DMIPrefixFilter] = deriveEncoder[DMIPrefixFilter]
  implicit val dmiExistsFilterEncoder: Encoder[DMIExistsFilter] = deriveEncoder[DMIExistsFilter]
  implicit val dmiContainsAnyFilterEncoder: Encoder[DMIContainsAnyFilter] =
    deriveEncoder[DMIContainsAnyFilter]
  implicit val dmiContainsAllFilterEncoder: Encoder[DMIContainsAllFilter] =
    deriveEncoder[DMIContainsAllFilter]

  implicit val dmiFilterEncoder: Encoder[DataModelInstanceFilter] = {
    case b: DMIBoolFilter =>
      b match {
        case f: DMIAndFilter => f.asJson
        case f: DMIOrFilter => f.asJson
        case f: DMINotFilter => f.asJson
      }
    case l: DMILeafFilter =>
      l match {
        case f: DMIInFilter => Json.obj(("in", f.asJson))
        case f: DMIEqualsFilter => Json.obj(("equals", f.asJson))
        case f: DMIRangeFilter => Json.obj(("range", f.asJson))
        case f: DMIPrefixFilter => Json.obj(("prefix", f.asJson))
        case f: DMIExistsFilter => Json.obj(("exists", f.asJson))
        case f: DMIContainsAnyFilter => Json.obj(("containsAny", f.asJson))
        case f: DMIContainsAllFilter => Json.obj(("containsAll", f.asJson))
      }
  }

  implicit val dataModelInstanceQueryEncoder: Encoder[DataModelInstanceQuery] =
    deriveEncoder[DataModelInstanceQuery]

  implicit val dataModelInstanceQueryResponseWithCursorDecoder
      : Decoder[DataModelInstanceQueryResponse] =
    deriveDecoder[DataModelInstanceQueryResponse]

  implicit val dataModelInstanceQueryResponseItemsWithCursorDecoder
      : Decoder[ItemsWithCursor[DataModelInstanceQueryResponse]] =
    deriveDecoder[ItemsWithCursor[DataModelInstanceQueryResponse]]

}
