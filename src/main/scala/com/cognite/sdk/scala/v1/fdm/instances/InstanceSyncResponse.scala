package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder

final case class InstanceSyncResponse(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    typing: Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]] =
      None,
    nextCursor: Map[String, String] = Map.empty
) {
  def getDataPart(): InstanceDataResponsePart = InstanceDataResponsePart(items, typing)

}

object InstanceSyncResponse {
  implicit val instanceSyncResponseEncoder: Encoder[InstanceSyncResponse] = deriveEncoder
  implicit val instanceSyncResponseDecoder: Decoder[InstanceSyncResponse] =
    InstanceDataResponsePart.instanceDataResponsePartDecoder
      .product(
        _.downField("nextCursor").as[Map[String, String]]
      )
      .map { case (data, cursor) => InstanceSyncResponse(data.items, data.typing, cursor) }
}
