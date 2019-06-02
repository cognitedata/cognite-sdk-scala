package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class FilesTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.files)
  it should behave like writableResource(
      client.files,
      Seq(File(name = "scala-sdk-read-example-1"), File(name = "scala-sdk-read-example-2")),
      Seq(CreateFile(name = "scala-sdk-read-example-1"), CreateFile(name = "scala-sdk-read-example-1"))
  )
}
