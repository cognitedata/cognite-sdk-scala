package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class FilesTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new GenericClient()
  private val idsThatDoNotExist = Seq(999991L, 999992L)
  it should behave like readableResource(client.files)
  it should behave like readableResourceWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = true)
  it should behave like writableResource(
    client.files,
    Seq(File(name = "scala-sdk-read-example-1")),
    Seq(CreateFile(name = "scala-sdk-read-example-1")),
    idsThatDoNotExist,
    supportsMissingAndThrown = true
  )
}
