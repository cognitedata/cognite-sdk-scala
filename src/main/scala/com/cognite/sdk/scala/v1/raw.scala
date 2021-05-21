// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common.{ToCreate, WithId}
import io.circe.Json

final case class RawDatabase(name: String) extends WithId[String] with ToCreate[RawDatabase] {
  override val id: String = this.name

  override def toCreate: RawDatabase = this
}

final case class RawTable(name: String) extends WithId[String] with ToCreate[RawTable] {
  override val id: String = this.name

  override def toCreate: RawTable = this
}

final case class RawRow(
    key: String,
    columns: Map[String, Json],
    lastUpdatedTime: Option[Instant] = None
) extends ToCreate[RawRow] {
  override def toCreate: RawRow = this
}

final case class RawRowKey(key: String)

final case class RawRowFilter(
    minLastUpdatedTime: Option[Instant] = None,
    maxLastUpdatedTime: Option[Instant] = None,
    columns: Option[Seq[String]] = None
)
