package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, InstanceId}

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
    val retrievedItem = testClient.files.retrieveByInstanceIds(Seq(instanceId)).unsafeRunSync()
    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedItem.headOption.map(_.createdTime) shouldNot be(empty)

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

}
