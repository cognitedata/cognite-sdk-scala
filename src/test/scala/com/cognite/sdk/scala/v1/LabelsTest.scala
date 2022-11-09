// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1
import com.cognite.sdk.scala.common.{ReadBehaviours, RetryWhile, SdkTestSpec, WritableBehaviors}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.IterableOps"
  )
)
class LabelsTest extends SdkTestSpec with ReadBehaviours with WritableBehaviors with RetryWhile {
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")
  private val labelDataSet = "test-ds-labels-scala-sdk"
  def createRandItems(): Seq[LabelCreate] = {
    val dataSet: Seq[DataSet] = client.dataSets.retrieveByExternalIds(Seq(labelDataSet), ignoreUnknownIds = true)
    val dataSetId = if (dataSet.isEmpty) {
      client.dataSets.createOne(DataSetCreate(name = Some(labelDataSet), externalId = Some(labelDataSet))).id
    } else {
      dataSet.head.id
    }
    Seq(
      LabelCreate(name = "scala-sdk-read-example-1", externalId = shortRandom()),
      LabelCreate(name = "scala-sdk-read-example-2", externalId = shortRandom(), dataSetId = Some(dataSetId))
    )
  }


  it should behave like writableWithRequiredExternalId(
    client.labels,
    Some(client.labels),
    Seq(
      Label(name = "scala-sdk-read-example-1", externalId = shortRandom()),
      Label(name = "scala-sdk-read-example-2", externalId = shortRandom())
    ),
    createRandItems(),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )

  it should "support filter" in {
    val randItems = createRandItems()

    // Cleanup existing items if they exist to be more resilient to previous failures
    val cleanupItems = randItems.flatMap(
      item => client.labels.filter(LabelsFilter(name = Some(item.name))).compile.toVector
    )
    client.labels.deleteByExternalIds(cleanupItems.map(_.externalId))

    client.labels.create(randItems)

    val externalIdPrefix = randItems.head.externalId
    val byExternalId =
      client.labels.filter(LabelsFilter(externalIdPrefix = Some(externalIdPrefix))).compile.toVector
    byExternalId should have length 1
    byExternalId.head.externalId should be(randItems.head.externalId)

    val name = randItems.last.name
    val byName = client.labels.filter(LabelsFilter(name = Some(name))).compile.toVector
    byName should have length 1
    byName.head.externalId should be(randItems.last.externalId)

    val dataSetId = client.dataSets.retrieveByExternalId(labelDataSet).id
    val byDataSetId = client.labels.filter(LabelsFilter(dataSetIds = Some(Seq(CogniteInternalId(dataSetId))))).compile.toVector
    byDataSetId should have length 1
    byDataSetId.head.externalId should be(randItems.last.externalId)

    val bydataSetExternalId = client.labels.filter(LabelsFilter(dataSetIds = Some(Seq(CogniteExternalId(labelDataSet))))).compile.toVector
    bydataSetExternalId should have length 1
    bydataSetExternalId.head.externalId should be(randItems.last.externalId)
  }
}
