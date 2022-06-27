// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.PropertyMap.createDynamicPropertyDecoder
import com.cognite.sdk.scala.v1._
import fs2.Stream
import io.circe.{Decoder, Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

class Edges[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with DeleteByExternalIds[F]
    with BaseUrl {

  import Edges._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/edges"

  def createItems(
      spaceExternalId: String,
      model: DataModelIdentifier,
      autoCreateStartNodes: Boolean = false,
      autoCreateEndNodes: Boolean = false,
      overwrite: Boolean = false,
      items: Seq[Edge]
  )(implicit F: Async[F]): F[Seq[PropertyMap]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    dataModels.retrieveByExternalIds(Seq(model.model), model.space.getOrElse("")).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val dataModelInstanceDecoder: Decoder[PropertyMap] =
        createDynamicPropertyDecoder(props)

      // for some reason scala complains dataModelInstanceDecoder doesn't seem to be used when
      //   derivedDecoder is used bellow, so an explicit decoder is defined instead
      implicit val dataModelInstanceItemsDecoder: Decoder[Items[PropertyMap]] =
        Decoder.forProduct1("items")(Items.apply[PropertyMap])
      val body = EdgeCreate(
        spaceExternalId,
        model,
        autoCreateStartNodes,
        autoCreateEndNodes,
        overwrite,
        items
      )
      requestSession.post[Seq[PropertyMap], Items[PropertyMap], EdgeCreate](
        body,
        uri"$baseUrl",
        value => value.items
      )
    }
  }

  def query(inputQuery: DataModelInstanceQuery): F[DataModelInstanceQueryResponse] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    implicit val edgeQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
      DataModelInstanceQueryResponse.createDecoderForQueryResponse()
    requestSession
      .post[DataModelInstanceQueryResponse, DataModelInstanceQueryResponse, DataModelInstanceQuery](
        inputQuery,
        uri"$baseUrl/list",
        value => value
      )
  }

  private[sdk] def queryWithCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int],
      @annotation.nowarn partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[PropertyMap]] =
    query(inputQuery.copy(cursor = cursor, limit = limit)).map {
      case DataModelInstanceQueryResponse(items, _, cursor) =>
        ItemsWithCursor(items, cursor)
    }

  private[sdk] def queryWithNextCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, PropertyMap] =
    Readable
      .pullFromCursor(cursor, limit, None, queryWithCursor(inputQuery, _, _, _))
      .stream

  def queryStream(
      inputQuery: DataModelInstanceQuery,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, PropertyMap] =
    queryWithNextCursor(inputQuery, None, limit)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      externalIds: Seq[String]
  ): F[DataModelInstanceQueryResponse] = {
    implicit val edgeQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
      DataModelInstanceQueryResponse.createDecoderForQueryResponse()

    requestSession.post[
      DataModelInstanceQueryResponse,
      DataModelInstanceQueryResponse,
      DataModelInstanceByExternalId
    ](
      DataModelInstanceByExternalId(externalIds.map(CogniteExternalId(_)), model),
      uri"$baseUrl/byids",
      value => value
    )
  }
}

object Edges {

  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    DataModels.dataModelIdentifierEncoder

  implicit val edgeCreateEncoder: Encoder[EdgeCreate] = deriveEncoder[EdgeCreate]

  implicit val edgeItemsEncoder: Encoder[Items[EdgeCreate]] = deriveEncoder[Items[EdgeCreate]]

  implicit val dataModelInstanceQueryEncoder: Encoder[DataModelInstanceQuery] =
    deriveEncoder[DataModelInstanceQuery]

  implicit val dataModelInstanceByExternalIdEncoder: Encoder[DataModelInstanceByExternalId] =
    deriveEncoder[DataModelInstanceByExternalId]

  implicit val dmiByExternalIdItemsWithIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]] =
    deriveEncoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]]

}
