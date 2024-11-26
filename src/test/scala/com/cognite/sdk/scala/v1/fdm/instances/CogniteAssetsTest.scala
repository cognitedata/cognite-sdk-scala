package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.sttp.RetryingBackend
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, FileDownloadInstanceId, FileDownloadLink, FileUploadInstanceId, GenericClient, InstanceId}
import sttp.client3.UriContext
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import java.io.{BufferedInputStream, FileInputStream}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.ThreadSleep"
  )
)
class CogniteAssetsTest extends CommonDataModelTestHelper {
  private lazy val client: GenericClient[IO] = GenericClient[IO](
    "scala-sdk-test",
    testClient.projectName,
    baseUrl,
    authProvider.getAuth.unsafeRunSync()
  )(
    implicitly,
    implicitly,
    new RetryingBackend[IO, Any](AsyncHttpClientCatsBackend[IO]().unsafeRunSync())
  )

  "cogniteAssets from core data modeling" should "create and delete files using CogniteFile" in {
    // create a single item
    val instanceId: InstanceId = InstanceId(space = Utils.SpaceExternalId, externalId = "file_instance_ext_id")
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

    val retrievedItem = testClient.files.retrieveByInstanceIds(Seq(instanceId)).unsafeRunSync()
    val retrievedSingleItem = testClient.files.retrieveByInstanceId(instanceId).unsafeRunSync()
    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.uploaded should be(false)
    retrievedItem.headOption.map(_.createdTime) shouldNot be(empty)

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

  it should "make it possible to retrieve file and associated upload link and download link using instance id" in {

    val instanceId: InstanceId = InstanceId(space = Utils.SpaceExternalId, externalId = "file_instance_ext_id2")

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
    val retrievedItem = testClient.files.retrieveByInstanceIds(Seq(instanceId)).unsafeRunSync()
    val retrievedSingleItem = testClient.files.retrieveByInstanceId(instanceId).unsafeRunSync()
    val uploadLinkFile = testClient.files.uploadLink(FileUploadInstanceId(instanceId)).unsafeRunSync()
    uploadLinkFile.uploadUrl shouldNot be(empty)
    val inputStream = new BufferedInputStream(
      new FileInputStream(
        new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt")
      )
    )
    uploadLinkFile.uploadUrl match {
      case Some(uploadUrl) =>
        val uploadResponse = client.requestSession.send { request =>
          request
            .body(inputStream)
            .put(uri"$uploadUrl")
        }.unsafeRunSync()
        print(uploadResponse.body)
      case _ => fail("No upload link received for tile")
    }
    Thread.sleep(10000)

    val downloadLink: FileDownloadLink = testClient.files.downloadLink(FileDownloadInstanceId(instanceId)).unsafeRunSync()
    downloadLink.downloadUrl shouldNot be(empty)
    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.uploaded should be(true)
    retrievedItem.headOption.map(_.createdTime) shouldNot be(empty)

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }
}
