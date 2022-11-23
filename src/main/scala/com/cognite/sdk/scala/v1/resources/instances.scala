// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.instances._
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, JsonObject, Printer}
import sttp.client3._
import sttp.client3.circe._

class Instances[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  private implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  override val baseUrl = uri"${requestSession.baseUrl}/models/instances"

  def createItems(containers: Seq[InstanceCreate]): F[Seq[SlimNodeOrEdge]] =
    requestSession
      .post[Seq[SlimNodeOrEdge], Items[SlimNodeOrEdge], Items[InstanceCreate]](
        Items(items = containers),
        uri"$baseUrl",
        value => value.items
      )

  def filter(filterRequest: InstanceFilterRequest): F[InstanceRetrieveResponse] =
    requestSession.post[InstanceRetrieveResponse, InstanceRetrieveResponse, InstanceFilterRequest](
      filterRequest,
      uri"$baseUrl/list",
      value => value
    )

  def retrieveByExternalIds(
      items: Seq[InstanceRetrieve],
      includeTyping: Boolean = false
  ): F[InstanceRetrieveResponse] =
    requestSession
      .post[InstanceRetrieveResponse, InstanceRetrieveResponse, InstanceRetrieveRequest](
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

}
