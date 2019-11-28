package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}

class ServiceAccountsTest extends SdkTest with ReadBehaviours {
  // We don't have permissions to list service accounts
  //"ServiceAccounts" should behave like readable(client.serviceAccounts)
}
