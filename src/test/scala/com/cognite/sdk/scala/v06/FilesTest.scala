package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}

class FilesTest extends SdkTest with ReadBehaviours {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readable(client.files)
  it should behave like readableWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = false)
}
