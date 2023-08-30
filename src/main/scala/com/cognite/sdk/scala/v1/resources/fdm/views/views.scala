// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources.fdm.views

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.RequestSession
import com.cognite.sdk.scala.v1.fdm.common.DataModelReference
import com.cognite.sdk.scala.v1.fdm.views.{ViewCreateDefinition, ViewDefinition}
import fs2.Stream
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.client3._
import sttp.client3.circe._

class Views[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Views._
  override val baseUrl = uri"${requestSession.baseUrl}/models/views"

  def createItems(items: Seq[ViewCreateDefinition]): F[Seq[ViewDefinition]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession
      .post[Seq[ViewDefinition], ItemsWithCursor[ViewDefinition], Items[ViewCreateDefinition]](
        Items(items),
        uri"$baseUrl",
        value => value.items
      )
  }

  def retrieveItems(
      items: Seq[DataModelReference],
      includeInheritedProperties: Option[Boolean] = None
  ): F[Seq[ViewDefinition]] =
    requestSession.post[Seq[ViewDefinition], Items[ViewDefinition], Items[DataModelReference]](
      Items(items),
      uri"$baseUrl/byids"
        .addParam("includeInheritedProperties", includeInheritedProperties.map(_.toString)),
      value => value.items
    )

  def deleteItems(externalIds: Seq[DataModelReference]): F[Seq[DataModelReference]] =
    requestSession
      .post[Seq[DataModelReference], Items[DataModelReference], Items[DataModelReference]](
        Items(externalIds),
        uri"$baseUrl/delete",
        value => value.items
      )

  def listWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      space: Option[String],
      includeGlobal: Option[Boolean],
      includeInheritedProperties: Option[Boolean],
      @annotation.nowarn partition: Option[Partition] = None
  ): F[ItemsWithCursor[ViewDefinition]] = {
    val uriWithParams = uri"$baseUrl/list"
      .addParam("cursor", cursor)
      .addParam("limit", limit.map(_.toString))
      .addParam("space", space)
      .addParam("includeGlobal", includeGlobal.map(_.toString))
      .addParam("includeInheritedProperties", includeInheritedProperties.map(_.toString))
    requestSession.get[ItemsWithCursor[ViewDefinition], ItemsWithCursor[ViewDefinition]](
      uriWithParams,
      value => value
    )
  }
  private[sdk] def listWithNextCursor(
      cursor: Option[String],
      limit: Option[Int],
      space: Option[String],
      includeGlobal: Option[Boolean],
      includeInheritedProperties: Option[Boolean]
  ): Stream[F, ViewDefinition] =
    Readable
      .pullFromCursor(
        cursor,
        limit,
        None,
        listWithCursor(_, _, space, includeGlobal, includeInheritedProperties, _)
      )
      .stream

  def listStream(
      limit: Option[Int],
      space: Option[String],
      includeGlobal: Option[Boolean],
      includeInheritedProperties: Option[Boolean]
  ): fs2.Stream[F, ViewDefinition] =
    listWithNextCursor(None, limit, space, includeGlobal, includeInheritedProperties)
}

object Views {
  implicit val dataModelReferenceDecoder: Decoder[DataModelReference] =
    deriveDecoder[DataModelReference]
  implicit val dataModelReferenceItemsDecoder: Decoder[Items[DataModelReference]] =
    deriveDecoder[Items[DataModelReference]]

  implicit val dataModelReferenceEncoder: Encoder[DataModelReference] =
    deriveEncoder[DataModelReference]
}
