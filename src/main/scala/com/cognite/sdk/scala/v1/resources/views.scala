// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import fs2.Stream
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe._
import io.circe.syntax.EncoderOps
import sttp.client3._
import sttp.client3.circe._

class Views[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import Views._
  override val baseUrl = uri"${requestSession.baseUrl}/models/views"

  def createItems(items: Seq[ViewCreateDefinition]): F[Seq[ViewDefinition]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    println(s"views = ${items.asJson.toString()}")
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

  def deleteItems(externalIds: Seq[DataModelReference]): F[Unit] =
    requestSession.post[Unit, Unit, Items[DataModelReference]](
      Items(externalIds),
      uri"$baseUrl/delete",
      _ => ()
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
  implicit val viewPropertyDefinition: Decoder[ViewPropertyDefinition] =
    deriveDecoder[ViewPropertyDefinition]
  implicit val viewDefinitionDecoder: Decoder[ViewDefinition] =
    deriveDecoder[ViewDefinition]
  implicit val viewDefinitionItemsDecoder: Decoder[Items[ViewDefinition]] =
    deriveDecoder[Items[ViewDefinition]]
  implicit val viewDefinitionItemsWithCursorDecoder: Decoder[ItemsWithCursor[ViewDefinition]] =
    deriveDecoder[ItemsWithCursor[ViewDefinition]]

  implicit val viewCreateDefinitionEncoder: Encoder[ViewCreateDefinition] =
    deriveEncoder[ViewCreateDefinition]
  implicit val viewCreateDefinitionItemsEncoder: Encoder[Items[ViewCreateDefinition]] =
    deriveEncoder[Items[ViewCreateDefinition]]

  implicit val dataModelReferenceItemsEncoder: Encoder[Items[DataModelReference]] =
    deriveEncoder[Items[DataModelReference]]
}