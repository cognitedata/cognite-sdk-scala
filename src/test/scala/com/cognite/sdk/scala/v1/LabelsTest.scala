package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common._

class LabelsTest extends SdkTestSpec with WritableBehaviors {
  private val externalIdsThatDoNotExist = Seq("5PNii0w4GCDBvXPZ", "6VhKQqtTJqBHGulw")

  it should behave like writableWithMandatoryExternalId(
    client.labels,
    Some(client.labels),
    Seq(
      Label(name = "scala-sdk-read-example-1", externalId = shortRandom()),
      Label(name = "scala-sdk-read-example-2", externalId = shortRandom())
    ),
    Seq(
      LabelCreate(name = "scala-sdk-create-example-1", externalId = shortRandom()),
      LabelCreate(name = "scala-sdk-create-example-2", externalId = shortRandom())
    ),
    externalIdsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
