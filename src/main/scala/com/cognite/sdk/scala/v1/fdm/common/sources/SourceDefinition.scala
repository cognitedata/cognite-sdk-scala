package com.cognite.sdk.scala.v1.fdm.common.sources

import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition
import com.cognite.sdk.scala.v1.fdm.instances.InstanceSource

trait SourceDefinition {
  def space: String
  def externalId: String
  def name: Option[String]
  def description: Option[String]
  def usedFor: Usage
  def properties: Map[String, PropertyDefinition]
  def createdTime: Long
  def lastUpdatedTime: Long

  def toSourceReference: SourceReference
  def toInstanceSource: InstanceSource = InstanceSource(toSourceReference)
}
