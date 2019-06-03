package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest}

class FilesTest extends SdkTest with ReadableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.files, supportsMissingAndThrown = false)
}
