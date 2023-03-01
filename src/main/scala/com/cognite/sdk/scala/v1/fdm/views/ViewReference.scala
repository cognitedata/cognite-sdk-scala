// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import com.cognite.sdk.scala.v1.fdm.common.sources.{SourceReference, SourceType}
import com.cognite.sdk.scala.v1.fdm.datamodels.DataModelViewReference
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Encoder}

final case class ViewReference(
    space: String,
    externalId: String,
    version: String
) extends SourceReference
    with DataModelViewReference {
  override val `type`: SourceType = SourceType.View
}

object ViewReference {
  implicit val viewReferenceEncoder: Encoder[ViewReference] =
    Encoder.forProduct4("type", "space", "externalId", "version")((c: ViewReference) =>
      (c.`type`, c.space, c.externalId, c.version)
    )

  implicit val viewReferenceDecoder: Decoder[ViewReference] = deriveDecoder[ViewReference]
}
