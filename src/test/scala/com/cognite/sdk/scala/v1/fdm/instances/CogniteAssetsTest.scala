package com.cognite.sdk.scala.v1.fdm.instances

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDeletionRequest.NodeDeletionRequest
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.NodeWrite
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, File, FileDownloadInstanceId, FileDownloadLink, FileUploadInstanceId, InstanceId}
import sttp.client3.UriContext

import java.io.{BufferedInputStream, FileInputStream}
import scala.annotation.tailrec
import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.ThreadSleep",
    "org.wartremover.warts.Equals"
  )
)
class CogniteAssetsTest extends CommonDataModelTestHelper {
  private val defaultAttemptCount: Int = 7

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
    val retrievedItem =
      retry[Seq[File]](() => testClient.files.retrieveByInstanceIds(Seq(instanceId)), files => files.headOption.map(_.createdTime).nonEmpty, defaultAttemptCount)

    val retrievedSingleItem =
      retry[File](() => testClient.files.retrieveByInstanceId(instanceId), _ => true, defaultAttemptCount)

    val uploadLinkFile =
      retry[File](() => testClient.files.uploadLink(FileUploadInstanceId(instanceId)), file => file.uploadUrl.nonEmpty, defaultAttemptCount)

    uploadLinkFile shouldBe a [Right[_, _]]

    val file = new java.io.File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt")
    val inputStream = new BufferedInputStream(
      new FileInputStream(
        file
      )
    )
    val fileSize = file.length()
    uploadLinkFile.map(_.uploadUrl) match {
      case Right(Some(uploadUrl)) =>
        testClient.requestSession.send { request =>
          request
            .contentLength(fileSize)
            .body(inputStream)
            .put(uri"$uploadUrl")
        }.unsafeRunSync()
      case _ => fail("No upload link received for tile")
    }

    val downloadLink: Either[Throwable, FileDownloadLink] =
      retry[FileDownloadLink](() => testClient.files.downloadLink(FileDownloadInstanceId(instanceId)), downloadLink => downloadLink.downloadUrl.nonEmpty, defaultAttemptCount)

    downloadLink.map(_.downloadUrl) match {
      case Right(value) => value should not be empty // Checks presence in `Right`
      case _            => fail("Could not get download link")
    }
    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.map(_.instanceId) should be(Right(Some(instanceId)))
    retrievedItem.map(_.headOption.flatMap(_.instanceId)) should be(Right(Some(instanceId)))

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

  //Retries a request until it succeeds and the test passes or there is no attempt left
  @tailrec
  private final def retry[T](requestToAttempt: () => IO[T], test: T => Boolean, attemptsLeft: Int): Either[Throwable, T] = {
    val result = Try(requestToAttempt().unsafeRunSync())
    if (result.map(test).getOrElse(false) && attemptsLeft > 0) {
      Thread.sleep(500)
      print("attempt")
      retry(requestToAttempt, test, attemptsLeft - 1)
    }
    else {
      result.toEither
    }
  }

}
