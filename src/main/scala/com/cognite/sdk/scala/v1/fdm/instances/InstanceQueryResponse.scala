package com.cognite.sdk.scala.v1.fdm.instances

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder

final case class InstanceQueryResponse(
    items: Option[Map[String, Seq[InstanceDefinition]]] = None,
    typing: Option[Map[String, Map[String, Map[String, Map[String, TypePropertyDefinition]]]]] =
      None,
    nextCursor: Option[Map[String, String]] = None
) {
  def getDataPart: InstanceDataResponsePart = InstanceDataResponsePart(items, typing)
}

object InstanceQueryResponse {
  implicit val instanceQueryResponseEncoder: Encoder[InstanceQueryResponse] = deriveEncoder
  implicit val instanceQueryResponseDecoder: Decoder[InstanceQueryResponse] =
    InstanceDataResponsePart.instanceDataResponsePartDecoder
      .product(
        _.downField("nextCursor").as[Option[Map[String, String]]]
      )
      .map { case (data, cursor) => InstanceQueryResponse(data.items, data.typing, cursor) }
}
