package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest, WritableBehaviors}

class FilesTest extends SdkTest with ReadBehaviours with WritableBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.files)
  it should behave like readableWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writable(
    client.files,
    Seq(File(name = "scala-sdk-read-example-1")),
    Seq(CreateFile(name = "scala-sdk-read-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
