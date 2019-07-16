package com.cognite.sdk.scala.v1

import java.util.UUID

import com.cognite.sdk.scala.common.{LoggingSttpBackend, ReadBehaviours, SdkTest, WritableBehaviors}
import com.softwaremill.sttp.Id

class FilesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, new LoggingSttpBackend[Id, Nothing](sttpBackend))
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  implicit val backend = new LoggingSttpBackend[Id, Nothing](sttpBackend)
  it should behave like readable(client.files)
  it should behave like readableWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writable(
    client.files,
    Seq(File(name = "scala-sdk-read-example-1")),
    Seq(CreateFile(name = "scala-sdk-read-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
  private val externalId = UUID.randomUUID().toString.substring(0, 8)
  private val filesToCreate = Seq(
    File(name = "scala-sdk-update-1", source = Some("scala-sdk-update-1"), externalId = Some(externalId), metadata = Some(Map()), assetIds = Some(Seq[Long]())
    )
  )
  private val fileUpdates = Seq(
    File(name = "scala-sdk-update-1-1", source = Some(null), externalId = Some(s"${externalId}-1")) // scalastyle:ignore null
  )
  it should behave like updatable(
    client.files,
    filesToCreate,
    fileUpdates,
    (id: Long, item: File) => item.copy(id = id),
    (a: File, b: File) => { a.copy(lastUpdatedTime = b.lastUpdatedTime) == b },
    (readFiles: Seq[File], updatedFiles: Seq[File]) => {
      assert(filesToCreate.size == fileUpdates.size)
      assert(readFiles.size == filesToCreate.size)
      assert(readFiles.size == updatedFiles.size)
      assert(updatedFiles.zip(readFiles).forall { case (updated, read) =>
        println(s"updated name is ${updated.externalId}, read name is ${read.externalId}") // scalastyle:ignore
        updated.externalId == read.externalId.map(id => s"${id}-1")
      })
      assert(readFiles.head.source.isDefined)
      assert(updatedFiles.head.source.isEmpty)
      ()
    }
  )
}
