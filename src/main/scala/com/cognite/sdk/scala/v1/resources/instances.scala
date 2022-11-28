// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.instances._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, JsonObject, Printer}
import sttp.client3._
import sttp.client3.circe._

class Instances[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  import Instances._

  private implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  override val baseUrl = uri"${requestSession.baseUrl}/models/instances"

  def createItems(containers: Seq[InstanceCreate]): F[Seq[SlimNodeOrEdge]] =
    requestSession
      .post[Seq[SlimNodeOrEdge], Items[SlimNodeOrEdge], Items[InstanceCreate]](
        Items(items = containers),
        uri"$baseUrl",
        value => value.items
      )

  def filter(filterRequest: InstanceFilterRequest): F[InstanceFilterResponse] =
    requestSession.post[InstanceFilterResponse, InstanceFilterResponse, InstanceFilterRequest](
      filterRequest,
      uri"$baseUrl/list",
      value => value
    )

  def retrieveByExternalIds(
      items: Seq[InstanceRetrieve],
      includeTyping: Boolean = false
  ): F[InstanceFilterResponse] =
    requestSession
      .post[InstanceFilterResponse, InstanceFilterResponse, InstanceRetrieveRequest](
        InstanceRetrieveRequest(items, includeTyping),
        uri"$baseUrl/byids",
        value => value
      )

  def delete(instanceRefs: Seq[InstanceDeleteRequest]): F[Unit] =
    requestSession.post[Unit, JsonObject, Items[InstanceDeleteRequest]](
      Items(items = instanceRefs),
      uri"$baseUrl/delete",
      _ => ()
    )
}

object Instances {
  implicit val instanceViewDataEncoder: Encoder[InstanceViewData] = deriveEncoder
  implicit val instanceContainerDataEncoder: Encoder[InstanceContainerData] = deriveEncoder
  implicit val directRelationReferenceEncoder: Encoder[DirectRelationReference] = deriveEncoder
  implicit val InstancePropertyDefinitionEncoder: Encoder[InstancePropertyDefinition] =
    deriveEncoder
  implicit val instanceRetrieveEncoder: Encoder[InstanceRetrieve] = deriveEncoder
  implicit val instanceRetrieveResponseEncoder: Encoder[InstanceFilterResponse] = deriveEncoder
  implicit val instanceDeleteRequestEncoder: Encoder[InstanceDeleteRequest] = deriveEncoder
  implicit val instanceDeleteRequestItemsEncoder: Encoder[Items[InstanceDeleteRequest]] =
    deriveEncoder
  implicit val instanceCreateEncoder: Encoder[InstanceCreate] = deriveEncoder
  implicit val viewPropertyReferenceEncoder: Encoder[ViewPropertyReference] = deriveEncoder
  implicit val propertySortV3Encoder: Encoder[PropertySortV3] = deriveEncoder
  implicit val itemsInstanceCreateEncoder: Encoder[Items[InstanceCreate]] = deriveEncoder
  implicit val instanceFilterRequestEncoder: Encoder[InstanceFilterRequest] = deriveEncoder
  implicit val instanceRetrieveRequestEncoder: Encoder[InstanceRetrieveRequest] = deriveEncoder

  implicit val instanceViewDataDecoder: Decoder[InstanceViewData] = deriveDecoder
  implicit val instanceContainerDataDecoder: Decoder[InstanceContainerData] = deriveDecoder
  implicit val directRelationReferenceDecoder: Decoder[DirectRelationReference] = deriveDecoder
  implicit val instanceRetrieveDecoder: Decoder[InstanceRetrieve] = deriveDecoder
  implicit val instancePropertyDefinitionDecoder: Decoder[InstancePropertyDefinition] =
    deriveDecoder
  implicit val instanceFilterResponseDecoder: Decoder[InstanceFilterResponse] = deriveDecoder
  implicit val instanceDeleteRequestDecoder: Decoder[InstanceDeleteRequest] = deriveDecoder
  implicit val slimNodeOrEdgeItemsDecoder: Decoder[Items[SlimNodeOrEdge]] = deriveDecoder
}
