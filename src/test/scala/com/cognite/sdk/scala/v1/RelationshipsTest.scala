// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1
import com.cognite.sdk.scala.common._

import java.time.temporal.ChronoUnit
import java.time.Instant
class RelationshipsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like readableWithRetrieveByRequiredExternalId(client.relationships, externalIdsThatDoNotExist, supportsMissingAndThrown = true)

  it should behave like writableWithRequiredExternalId(
    client.relationships,
    Some(client.relationships),
    Seq(
      Relationship(
        sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        externalId = shortRandom()
      ),
      Relationship(
        sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        externalId = shortRandom()
      )
    ),
    Seq(
      RelationshipCreate(
        sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        externalId = shortRandom()
      ),
      RelationshipCreate(
        sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        externalId = shortRandom()
      )
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true,
    trySameIdsThatDoNotExist = false
  )

  private val randomExternalId = shortRandom()

  it should behave like updatableByRequiredExternalId(
    client.relationships,
    None,
    Seq(
      Relationship(
        sourceExternalId = "relationships-update-test-1",
        sourceType = "asset",
        targetExternalId = "scala-sdk-relationships-update-test-asset2",
        targetType = "asset",
        startTime = Some(Instant.ofEpochMilli(1605866626000L)),
        endTime = Some(Instant.ofEpochMilli(1606125826000L)),
        labels = Some(Seq(CogniteExternalId("scala-sdk-relationships-test-label1"))),
        externalId = s"update-1-externalId-$randomExternalId",
        dataSetId = Some(2694232156565845L)
      ),
      Relationship(
        sourceExternalId = "relationships-update-test-2",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-update-test-event2",
        targetType = "event",
        confidence = Some(0.6),
        labels = Some(Seq(
          CogniteExternalId("scala-sdk-relationships-test-label1"),
          CogniteExternalId("scala-sdk-relationships-test-label2"))
        ),
        startTime = Some(Instant.ofEpochMilli(1602354975000L)),
        endTime = Some(Instant.ofEpochMilli(1602527775000L)),
        externalId = s"update-2-externalId-$randomExternalId",
        dataSetId = Some(2694232156565845L)
      )
    ),
    Map(s"update-1-externalId-$randomExternalId" -> RelationshipUpdate(sourceExternalId = Some(SetValue(s"relationships-update-test-1-1"))),
      s"update-2-externalId-$randomExternalId" -> RelationshipUpdate(sourceExternalId = Some(SetValue(s"relationships-update-test-2-1")))),
    (readRelationships: Seq[Relationship], updatedRelationships: Seq[Relationship]) => {
      assert(readRelationships.size == updatedRelationships.size)
      assert(updatedRelationships.zip(readRelationships).forall { case (updated, read) =>
        updated.sourceExternalId === s"${read.sourceExternalId}-1" })
      ()
    }
  )

  it should "support filter" in {

    val existingItems = client.relationships
      .filterWithCursor(RelationshipsFilter(dataSetIds = Some(Seq(CogniteInternalId(2694232156565845L)))), None, None, None, None)
      .items.map(_.externalId)

    client.relationships.deleteByExternalIds(externalIds = existingItems, ignoreUnknownIds = true)
    val randomItems = Seq(
      RelationshipCreate(
        sourceExternalId = "scala-sdk-relationships-test-asset1",
        sourceType = "asset",
        targetExternalId = "scala-sdk-relationships-test-asset2",
        targetType = "asset",
        startTime = Some(Instant.ofEpochMilli(1605866626000L)),
        endTime = Some(Instant.ofEpochMilli(1606125826000L)),
        labels = Some(Seq(CogniteExternalId("scala-sdk-relationships-test-label1"))),
        externalId = "scala-sdk-relationships-test-example-1",
        dataSetId = Some(2694232156565845L)
      ),
      RelationshipCreate(sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        confidence = Some(0.6),
        labels = Some(Seq(
          CogniteExternalId("scala-sdk-relationships-test-label1"),
          CogniteExternalId("scala-sdk-relationships-test-label2"))
        ),
        startTime = Some(Instant.ofEpochMilli(1602354975000L)),
        endTime = Some(Instant.ofEpochMilli(1602527775000L)),
        externalId = "scala-sdk-relationships-test-example-2",
        dataSetId = Some(2694232156565845L)
      ),
      RelationshipCreate(sourceExternalId = "scala-sdk-relationships-test-event1",
        sourceType = "event",
        targetExternalId = "scala-sdk-relationships-test-event2",
        targetType = "event",
        confidence = Some(0.8),
        startTime = Some(Instant.ofEpochMilli(1605866626000L)),
        externalId = "scala-sdk-relationships-test-example-3",
        dataSetId = Some(2694232156565845L)
      )
    )
    client.relationships.create(randomItems)


    val minAge = Instant.now().minus(10, ChronoUnit.MINUTES)
    val createdTimeRange = Some(
      TimeRange(min = Some(minAge))
    )
    val dataSetIds = Some(Seq(CogniteInternalId(2694232156565845L)))
    val createdTimeFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds
        )
      )
      .compile
      .toList
    assert(createdTimeFilterResults.length == 3)

    val createdTimeFilterResultsLimit = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds
        ),
        limit = Some(1)
      )
      .compile
      .toList
    assert(createdTimeFilterResultsLimit.length == 1)

    val targetFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          targetExternalIds = Some(Seq("scala-sdk-relationships-test-event2")),
          targetTypes = Some(Seq("event"))
        )
      )
      .compile
      .toList
    assert(targetFilterResults.length == 2)

    // labels
    val sourceTypeFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          sourceTypes = Some(Seq("event", "asset"))
        )
      )
      .compile
      .toList
    assert(sourceTypeFilterResults.length == 3)

    val startTimeFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          startTime = Some(
            TimeRange(min = Some(Instant.ofEpochMilli(1605866626000L)), max = Some(Instant.ofEpochMilli(1605866626010L)))
          )
        )
      )
      .compile
      .toList
    assert(startTimeFilterResults.length == 2)

    val activeAtTimeFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          activeAtTime = Some(TimeRange(min = Some(Instant.ofEpochMilli(1606125826000L))))
        )
      )
      .compile
      .toList
    assert(activeAtTimeFilterResults.length == 2)

    val confidenceRangeFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          confidence = Some(ConfidenceRange(max = Some(0.7)))
        )
      )
      .compile
      .toList
    assert(confidenceRangeFilterResults.length == 1)

    val labels = Seq(
      CogniteExternalId("scala-sdk-relationships-test-label1"),
      CogniteExternalId("scala-sdk-relationships-test-label2")
    )
    val containsAnyFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          labels = Some(ContainsAny(containsAny = labels))
        )
      )
      .compile
      .toList
    assert(containsAnyFilterResults.length == 2)

    val containsAllFilterResults = client.relationships
      .filter(
        RelationshipsFilter(
          createdTime = createdTimeRange,
          dataSetIds = dataSetIds,
          labels = Some(ContainsAll(containsAll = labels))
        )
      )
      .compile
      .toList
    assert(containsAllFilterResults.length == 1)
  }
}
