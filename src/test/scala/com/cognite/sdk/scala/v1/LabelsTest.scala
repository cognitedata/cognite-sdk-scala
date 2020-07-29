package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{SdkTestSpec, WritableBehaviors}
import org.scalatest.Matchers

class LabelsTest extends SdkTestSpec with WritableBehaviors with Matchers {
  it should behave like writable(
    client.labels,
    Some(client.labels),
    Seq(
      Label(externalId = "scala-sdk-read-example-1", name = "scala-sdk-read-example-1"),
      Label(externalId = "scala-sdk-read-example-2", name = "scala-sdk-read-example-2")
    ),
    Seq(
      LabelCreate(externalId = "scala-sdk-read-example-1", name = "scala-sdk-read-example-1"),
      LabelCreate(externalId = "scala-sdk-read-example-2", name = "scala-sdk-read-example-2")
    ),
    supportsMissingAndThrown = true
  )

}
