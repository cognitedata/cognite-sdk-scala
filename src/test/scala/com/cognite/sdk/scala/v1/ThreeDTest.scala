package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class ThreeDTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()(auth, backend)
  private val idsThatDoNotExist = Seq(9999991L, 9999992L)
  ("ThreeDModels" should behave).like(readableResource(client.threeDModels))
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
  ("ThreeDRevisions" should behave).like(
    readableResource(client.threeDRevisions(4222532244684431L))
  )
  (it should behave).like(
    writableResource(
      client.threeDRevisions(4222532244684431L),
      Seq(
        ThreeDRevision(
          fileId = 6528506295318577L,
          id = 7052773602935837L,
          published = false,
          status = "Done",
          assetMappingCount = 0,
          createdTime = 1550739713
        ),
        ThreeDRevision(
          fileId = 8440701612364206L,
          id = 8685851966685955L,
          published = false,
          status = "Done",
          assetMappingCount = 0,
          createdTime = 1550739711
        )
      ),
      Seq(
        CreateThreeDRevision(published = false, fileId = 8440701612364206L),
        CreateThreeDRevision(published = false, fileId = 6528506295318577L)
      ),
      idsThatDoNotExist,
      false
    )
  )
  ("ThreeDAssetMapping" should behave).like(
    readableResource(client.threeDAssetMappings(1367881358941595L, 7901013305364074L))
  )
}
