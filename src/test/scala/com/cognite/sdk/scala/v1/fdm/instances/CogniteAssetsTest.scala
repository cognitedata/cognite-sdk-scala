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
  private val defaultAttemptCount: Int = 10

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
      retry[Seq[File]](() => testClient.files.retrieveByInstanceIds(Seq(instanceId)), defaultAttemptCount)

    val retrievedSingleItem =
      retry[File](() => testClient.files.retrieveByInstanceId(instanceId), defaultAttemptCount)

    val uploadLinkFile =
      retry[File](() => testClient.files.uploadLink(FileUploadInstanceId(instanceId)), defaultAttemptCount)

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
      case Right(None) => fail("no upload url returned by uploadLink")
      case Left(e) => fail(e.getMessage)
    }

    retryWithTest[FileDownloadLink](() => testClient.files.downloadLink(FileDownloadInstanceId(instanceId)), downloadLink => downloadLink.downloadUrl.nonEmpty, defaultAttemptCount, "empty download link")

    createdItem.headOption.flatMap(_.createdTime) shouldNot be(empty)
    retrievedSingleItem.map(_.instanceId) should be(Right(Some(instanceId)))
    retrievedItem.map(_.headOption.flatMap(_.instanceId)) should be(Right(Some(instanceId)))

    testClient.instances.delete(Seq(NodeDeletionRequest(instanceId.space, instanceId.externalId))).unsafeRunSync()
  }

  //Retries a request until it succeeds and the test passes or there is no attempt left
  @tailrec
  private final def retryWithTest[T](requestToAttempt: () => IO[T], test: T => Boolean, attemptsLeft: Int, testFailedMessage: String): Either[Throwable, T] = {
    val result = Try(requestToAttempt().unsafeRunSync()).toEither

    result match {
      case Right(value) if test(value) => Right(value) // Successful test
      case _ if attemptsLeft > 0 =>
        Thread.sleep(500)
        retryWithTest(requestToAttempt, test, attemptsLeft - 1, testFailedMessage) // Retry on failure
      case Left(error) => Left(new IllegalStateException("Request still fails after exhausting retries", error))
      case Right(_)    => Left(new IllegalStateException(s"Request still fails test after exhausting retries, test failed message: $testFailedMessage"))
    }
  }

  private def retry[T](requestToAttempt: () => IO[T], attemptsLeft: Int): Either[Throwable, T] = {
    retryWithTest[T](requestToAttempt, _ => true, attemptsLeft, "")
  }


}
