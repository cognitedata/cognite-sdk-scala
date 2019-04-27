package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.SdkTest

class FilesTest extends SdkTest {
  it should "be possible to retrieve a file" in {
    val client = new Client()
    val files = client.files.read()
    println(files.unsafeBody.items.headOption.map(_.fileName).getOrElse("error")) // scalastyle:ignore
  }

  it should "fetch all files" in {
    val client = new Client()
    val files = client.files.readAll()
    val f = files.toSeq
    println(f.map(_.length).mkString(", ")) // scalastyle:ignore
    println(f.flatten.length) // scalastyle:ignore
  }
}
