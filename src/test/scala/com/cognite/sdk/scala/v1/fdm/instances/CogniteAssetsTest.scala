package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, FileUploadInstanceId, InstanceId}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements"
  )
)
class CogniteAssetsTest extends CommonDataModelTestHelper {
  "fdm" should "create and delete file using CogniteFile" in {
    // create a single item
    val instanceId: InstanceId = InstanceId(space = Utils.SpaceExternalId, externalId = "file_instance_ext_id")
    val testFile = InstanceCreate(
      items = Seq(NodeWrite(space = instanceId.space, externalId = instanceId.externalId, Some(Seq(EdgeOrNodeData(ViewReference("cdf_cdm", "CogniteFile", "v1"), None))), None)),
      None,
      None,
      None,
      None
    )
    val createdItem = testClient.instances.createItems(testFile).unsafeRunSync()
    val uploadLinkFile = testClient.files.uploadLink(FileUploadInstanceId(instanceId)).unsafeRunSync()
    uploadLinkFile.uploadUrl shouldNot be(empty)
    val retrievedItem = testClient.files.retrieveByInstanceIds(Seq(instanceId)).unsafeRunSync()
    val retrievedSingleItem = testClient.files.retrieveByInstanceId(instanceId).unsafeRunSync()
    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.uploaded should be(false)
    retrievedItem.headOption.map(_.createdTime) shouldNot be(empty)

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

}
