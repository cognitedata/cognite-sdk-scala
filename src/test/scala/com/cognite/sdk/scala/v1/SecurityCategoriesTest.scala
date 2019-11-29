package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}
// FIXME: Not sure why @Ignore isn't working. "behave like" doesn't seem to support ignore.
//import org.scalatest.Ignore
//
//@Ignore
class SecurityCategoriesTest extends SdkTest with ReadBehaviours {
  // TODO: We don't have any security categories in publicdata,
  //  and lack permissions to list them in greenfield
//  "SecurityCategories" should behave like readable(client.securityCategories, supportsLimit = false)
}
