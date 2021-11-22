// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._

final case class Label(
    externalId: Some[String],
    name: String,
    description: Option[String] = None,
    createdTime: Instant = Instant.ofEpochMilli(0)
) extends WithRequiredExternalId
    with WithCreatedTime
    with ToCreate[LabelCreate] {
  override def toCreate: LabelCreate =
    LabelCreate(
      externalId,
      name,
      description
    )
}

final case class LabelCreate(
    externalId: Some[String],
    name: String,
    description: Option[String] = None
) extends WithRequiredExternalId

final case class LabelsFilter(name: Option[String] = None, externalIdPrefix: Option[String] = None)
