package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{ReadableResourceBehaviors, SdkTest, WritableResourceBehaviors}

class FilesTest extends SdkTest with ReadableResourceBehaviors with WritableResourceBehaviors {
  private val client = new Client()

  it should behave like readableResource(client.files, supportsMissingAndThrown = false)
}
