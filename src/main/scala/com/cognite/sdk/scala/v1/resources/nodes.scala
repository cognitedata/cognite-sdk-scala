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

class Nodes[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with BaseUrl {
  import Nodes._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/nodes"

  def createItems(
      spaceExternalId: String,
      model: DataModelIdentifier,
      overwrite: Boolean = false,
      items: Seq[Node]
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

      requestSession
        .post[Seq[PropertyMap], Items[PropertyMap], DataModelNodeCreate](
          DataModelNodeCreate(spaceExternalId, model, overwrite, items),
          uri"$baseUrl",
          value => value.items
        )
    }
  }

  def query(
      inputQuery: DataModelInstanceQuery
  ): F[DataModelInstanceQueryResponse] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    implicit val nodeQueryReponseDecoder: Decoder[DataModelInstanceQueryResponse] =
      DataModelInstanceQueryResponse.createDecoderForQueryResponse()

    requestSession.post[
      DataModelInstanceQueryResponse,
      DataModelInstanceQueryResponse,
      DataModelInstanceQuery
    ](
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

  def deleteItems(externalIds: Seq[String], spaceExternalId: String): F[Unit] =
    requestSession.post[Unit, Unit, SpacedItems[CogniteId]](
      SpacedItems(spaceExternalId, externalIds.map(CogniteExternalId(_))),
      uri"$baseUrl/delete",
      _ => ()
    )

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      spaceExternalId: String,
      externalIds: Seq[String]
  ): F[DataModelInstanceQueryResponse] = {
    implicit val nodeQueryReponseDecoder: Decoder[DataModelInstanceQueryResponse] =
      DataModelInstanceQueryResponse.createDecoderForQueryResponse()

    requestSession.post[
      DataModelInstanceQueryResponse,
      DataModelInstanceQueryResponse,
      DataModelInstanceByExternalId
    ](
      DataModelInstanceByExternalId(spaceExternalId, externalIds.map(CogniteExternalId(_)), model),
      uri"$baseUrl/byids",
      value => value
    )
  }
}

object Nodes {

  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    DataModels.dataModelIdentifierEncoder

  implicit val dataModelNodeCreateEncoder: Encoder[DataModelNodeCreate] =
    deriveEncoder[DataModelNodeCreate]

  implicit val dataModelNodeItemsEncoder: Encoder[Items[DataModelNodeCreate]] =
    deriveEncoder[Items[DataModelNodeCreate]]

  implicit val dataModelInstanceQueryEncoder: Encoder[DataModelInstanceQuery] =
    deriveEncoder[DataModelInstanceQuery]

  implicit val dataModelInstanceByExternalIdEncoder: Encoder[DataModelInstanceByExternalId] =
    deriveEncoder[DataModelInstanceByExternalId]

  implicit val cogniteIdSpacedItemsEncoder: Encoder[SpacedItems[CogniteId]] =
    deriveEncoder[SpacedItems[CogniteId]]
}
