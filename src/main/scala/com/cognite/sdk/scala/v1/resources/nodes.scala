// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import fs2.Stream
import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Printer}
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

import scala.collection.immutable

class Nodes[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with DeleteByExternalIds[F]
    with BaseUrl {
  import Nodes._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/nodes"

  private def createDecoderForQueryResponse(): Decoder[DataModelInstanceQueryResponse] = {
    import DataModels.dataModelPropertyDefinitionDecoder

    (c: HCursor) =>
      for {
        modelProperties <- c
          .downField("modelProperties")
          .as[Map[String, DataModelPropertyDefinition]]

        seqDecoder: Decoder[Seq[PropertyMap]] =
          Decoder.decodeIterable[PropertyMap, Seq](
            createDynamicPropertyDecoder(modelProperties),
            implicitly
          )

        items <- seqDecoder.tryDecode(c.downField("items"))
        nextCursor <- c.downField("nextCursor").as[Option[String]]
      } yield DataModelInstanceQueryResponse(items, Some(modelProperties), nextCursor)
  }

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
      createDecoderForQueryResponse()

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

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      externalIds: Seq[String]
  ): F[DataModelInstanceQueryResponse] = {
    implicit val nodeQueryReponseDecoder: Decoder[DataModelInstanceQueryResponse] =
      createDecoderForQueryResponse()

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

  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf")
  )
  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]],
      props: Map[String, DataModelPropertyDefinition]
  ): Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
    res.filter {
      case Right(_) => true
      case Left(DecodingFailure("Attempt to decode value on failed cursor", downfields)) =>
        val nullableProps = downfields
          .filter(_.isInstanceOf[DownField])
          .map(_.asInstanceOf[DownField])
          .map(_.k)
          .filter(x => x.neqv("properties") && x.neqv("items"))
          .toSet
        !nullableProps.subsetOf(props.filter(_._2.nullable).keySet)
      case _ => true
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  // scalastyle:off cyclomatic.complexity
  def createDynamicPropertyDecoder(
      props: Map[String, DataModelPropertyDefinition]
  ): Decoder[PropertyMap] = (c: HCursor) => {
    val res: immutable.Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
      props.map { case (prop, dmp) =>
        for {
          value <- dmp.`type`.decodeProperty(c.downField(prop))
        } yield prop -> value
      }
    filterOutNullableProps(res, props).find(_.isLeft) match {
      case Some(Left(x)) => Left(x)
      case _ => Right(new PropertyMap(res.collect { case Right(value) => value }.toMap))
    }
  }
}
