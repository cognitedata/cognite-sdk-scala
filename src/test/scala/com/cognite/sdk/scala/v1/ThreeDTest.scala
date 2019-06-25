package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class ThreeDTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()(auth, backend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  (it should behave).like(readableResource(client.threeDModels))
  (it should behave).like(
    writableResource(
      client.threeDModels,
      Seq(
        ThreeDModel(name = "scala-sdk-threeD-read-example-1"),
        ThreeDModel(name = "scala-sdk-threeD-read-example-2")
      ),
      Seq(
        CreateThreeDModel(name = "scala-sdk-threeD-create-example-1"),
        CreateThreeDModel(name = "scala-sdk-threeD-create-example-2")
      ),
      idsThatDoNotExist,
      supportsMissingAndThrown = false
    )
  )
}
