// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.PropertyMap.createDynamicPropertyDecoder
import com.cognite.sdk.scala.v1._
import fs2.Stream
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Printer}
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

final case class EdgeQuery(
    model: DataModelIdentifier,
    filter: DomainSpecificLanguageFilter = EmptyFilter,
    sort: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    cursor: Option[String] = None
)

final case class EdgeQueryResponse(
    items: Seq[PropertyMap],
    modelProperties: Option[Map[String, DataModelPropertyDefinition]] = None,
    nextCursor: Option[String] = None
)

@SuppressWarnings(
  Array(
    "org.wartremover.warts.StringPlusAny"
  )
)
class Edges[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with DeleteByExternalIds[F]
    with BaseUrl {

  import Edges._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/edges"

  // TODO refactor this
  private def createDecoderForQueryResponse(): Decoder[EdgeQueryResponse] = {
    import DataModels.dataModelPropertyDefinitionDecoder
    new Decoder[EdgeQueryResponse] {
      def apply(c: HCursor): Decoder.Result[EdgeQueryResponse] = {
        val modelProperties = c
          .downField("modelProperties")
          .as[Option[Map[String, DataModelPropertyDefinition]]]
        modelProperties.flatMap { props =>
          implicit val propertyTypeDecoder: Decoder[PropertyMap] =
            createDynamicPropertyDecoder(props.getOrElse(Map()))
          for {
            items <- c.downField("items").as[Seq[PropertyMap]]
            nextCursor <- c.downField("nextCursor").as[Option[String]]
          } yield EdgeQueryResponse(items, props, nextCursor)
        }
      }
    }
  }

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

  def query(inputQuery: EdgeQuery): F[EdgeQueryResponse] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    implicit val edgeQueryResponseDecoder: Decoder[EdgeQueryResponse] =
      createDecoderForQueryResponse()

    println(s" bodyQuery = ${inputQuery.asJson}")

    requestSession.post[EdgeQueryResponse, EdgeQueryResponse, EdgeQuery](
      inputQuery,
      uri"$baseUrl/list",
      value => value
    )
  }

  private[sdk] def queryWithCursor(
      inputQuery: EdgeQuery,
      cursor: Option[String],
      limit: Option[Int],
      @annotation.nowarn partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[PropertyMap]] =
    query(inputQuery.copy(cursor = cursor, limit = limit)).map {
      case EdgeQueryResponse(items, _, cursor) =>
        ItemsWithCursor(items, cursor)
    }

  private[sdk] def queryWithNextCursor(
      inputQuery: EdgeQuery,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, PropertyMap] =
    Readable
      .pullFromCursor(cursor, limit, None, queryWithCursor(inputQuery, _, _, _))
      .stream

  def queryStream(
      inputQuery: EdgeQuery,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, PropertyMap] =
    queryWithNextCursor(inputQuery, None, limit)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      externalIds: Seq[String]
  ): F[EdgeQueryResponse] = {
    implicit val edgeQueryResponseDecoder: Decoder[EdgeQueryResponse] =
      createDecoderForQueryResponse()

    requestSession.post[
      EdgeQueryResponse,
      EdgeQueryResponse,
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

  implicit val dataModelInstanceQueryEncoder: Encoder[EdgeQuery] = deriveEncoder[EdgeQuery]

  implicit val dataModelInstanceByExternalIdEncoder: Encoder[DataModelInstanceByExternalId] =
    deriveEncoder[DataModelInstanceByExternalId]

  implicit val dmiByExternalIdItemsWithIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]] =
    deriveEncoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]]

}
