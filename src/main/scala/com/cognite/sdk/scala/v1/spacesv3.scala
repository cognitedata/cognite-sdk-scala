package com.cognite.sdk.scala.v1

import java.time.Instant

final case class SpaceCreateDefinition(
    space: String,
    description: Option[String] = None,
    name: Option[String] = None
)

final case class SpaceDefinition(
    space: String,
    description: Option[String] = None,
    name: Option[String] = None,
    createdTime: Instant,
    lastUpdatedTime: Instant
)

final case class SpaceById(space: String)
