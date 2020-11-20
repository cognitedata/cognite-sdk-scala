// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant

sealed trait CogniteId

final case class CogniteExternalId(externalId: String) extends CogniteId
final case class CogniteInternalId(id: Long) extends CogniteId

// min and max need to be optional, since one of them can be provided alone.
final case class TimeRange(min: Option[Instant] = None, max: Option[Instant] = None)
final case class ConfidenceRange(min: Option[Double] = None, max: Option[Double] = None)

// Used for filtering by label, labels: {containsAny: [{externalId: "label1"}, {externalId: "label2}]}
// or labels: {containsAll: [{externalId: "label1"}, {externalId: "label2}]}
final case class LabelContainsFilter(
    containsAny: Option[Seq[CogniteExternalId]] = None,
    containsAll: Option[Seq[CogniteExternalId]] = None
)

// Used for updating labels on a data type (only assets for now), examples:
// labels: {add: [{externalId: "label1"}, {externalId: "label2}]}, remove: [{externalId: "label3"}]}
// labels: {add: [{externalId: "label1"}, {externalId: "label2}]}}
final case class LabelsOnUpdate(
    add: Option[Seq[CogniteExternalId]] = None,
    remove: Option[Seq[CogniteExternalId]] = None
)
