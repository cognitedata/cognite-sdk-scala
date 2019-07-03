package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.WithId
import io.circe.Json

final case class RawDatabase(name: String) extends WithId[String] {
  override val id: String = this.name
}

final case class RawTable(name: String) extends WithId[String] {
  override val id: String = this.name
}

final case class RawRow(
    key: String,
    columns: Map[String, Json],
    lastUpdatedTime: Option[Long] = None
)

final case class RawRowKey(key: String)
