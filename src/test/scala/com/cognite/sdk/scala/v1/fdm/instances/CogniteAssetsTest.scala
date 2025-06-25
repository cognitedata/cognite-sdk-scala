package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.{CogniteInstanceId, CommonDataModelTestHelper, File, FileDownloadInstanceId, FileDownloadLink, FileUploadInstanceId, InstanceId}
import sttp.client3.UriContext


@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.ThreadSleep",
    "org.wartremover.warts.Equals"
  )
)
class CogniteAssetsTest extends CommonDataModelTestHelper with RetryWhile {

  it should "make it possible to retrieve file and associated upload link and download link using instance id" in {

    val instanceId: InstanceId = InstanceId(space = Utils.SpaceExternalId, externalId = "file_instance_ext_id2")
    val cogniteInstanceId: CogniteInstanceId = CogniteInstanceId(InstanceId(space = Utils.SpaceExternalId, externalId = "file_instance_ext_id2"))

    val testFile = InstanceCreate(
      items = Seq(
        NodeWrite(
          space = instanceId.space,
          externalId = instanceId.externalId,
          Some(Seq(EdgeOrNodeData(ViewReference("cdf_cdm", "CogniteFile", "v1"), None))
          ),
          None
        )),
      None,
      None,
      None,
      None
    )
    val createdItem = testClient.instances.createItems(testFile).unsafeRunSync()
    val retrievedItem =
      retry[Seq[File]](testClient.files.retrieveByInstanceIds(Seq(cogniteInstanceId)).unsafeRunSync())

    val retrievedSingleItem =
      retry[File](testClient.files.retrieveByInstanceId(cogniteInstanceId).unsafeRunSync())

    val uploadLinkFile =
      retry[File](testClient.files.uploadLink(FileUploadInstanceId(instanceId)).unsafeRunSync())

    val inputStream = getClass.getResourceAsStream("/uploadTest.txt")
    val fileSize = new java.io.File(getClass.getResource("/uploadTest.txt").getPath).length()


    uploadLinkFile.uploadUrl match {
      case Some(uploadUrl) =>
        testClient.requestSession.send { request =>
          request
            .contentLength(fileSize)
            .body(inputStream)
            .put(uri"$uploadUrl")
        }.unsafeRunSync()
      case None => fail("no upload url returned by uploadLink")
    }

    retryWithExpectedResult[FileDownloadLink](testClient.files.downloadLink(FileDownloadInstanceId(instanceId)).unsafeRunSync(), downloadLink => downloadLink.downloadUrl should not be(empty), retryOnException = true)

    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.instanceId should be(Some(instanceId))
    retrievedItem.headOption.flatMap(_.instanceId) should be(Some(instanceId))

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

  private def retry[A](action: => A): A = {
    retryWithExpectedResult[A](action, _ => succeed, retryOnException = true)
  }

}
