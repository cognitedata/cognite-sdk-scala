package com.cognite.sdk.scala.v1

import java.time.Instant

case class SpaceCreateDefinition(
    space: String,
    description: Option[String] = None,
    name: Option[String] = None
)

case class SpaceDefinition(
    space: String,
    description: Option[String] = None,
    name: Option[String] = None,
    createdTime: Instant,
    lastUpdatedTime: Instant
)

case class SpaceById(space: String)
