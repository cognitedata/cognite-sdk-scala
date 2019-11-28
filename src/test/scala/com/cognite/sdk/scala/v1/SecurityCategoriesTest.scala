package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}

class SecurityCategoriesTest extends SdkTest with ReadBehaviours {
  "SecurityCategories" should behave like readable(client.securityCategories)
}
