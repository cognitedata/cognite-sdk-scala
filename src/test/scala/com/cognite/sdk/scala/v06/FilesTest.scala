package com.cognite.sdk.scala.v06

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}
import io.circe.generic.auto._

class FilesTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new GenericClient()(auth, sttpBackend)
  private val idsThatDoNotExist = Seq(999991L, 999992L)

  it should behave like readableResource(client.files)
  it should behave like readableResourceWithRetrieve(client.files, idsThatDoNotExist, supportsMissingAndThrown = false)
}
