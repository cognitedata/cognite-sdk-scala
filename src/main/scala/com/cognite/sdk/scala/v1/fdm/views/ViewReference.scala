// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import com.cognite.sdk.scala.v1.fdm.SourceReference
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Encoder}

final case class ViewReference(
    space: String,
    externalId: String,
    version: String
) extends SourceReference {
  override val `type`: String = ViewReference.`type`
}

object ViewReference {
  val `type`: String = "view"

  implicit val viewReferenceEncoder: Encoder[ViewReference] =
    Encoder.forProduct4("type", "space", "externalId", "version")((c: ViewReference) =>
      (c.`type`, c.space, c.externalId, c.version)
    )

  implicit val viewReferenceDecoder: Decoder[ViewReference] = deriveDecoder[ViewReference]
}
