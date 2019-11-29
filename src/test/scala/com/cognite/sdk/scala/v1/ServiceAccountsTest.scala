package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}
// FIXME: Not sure why @Ignore isn't working. "behave like" doesn't seem to support ignore.
//import org.scalatest.Ignore
//
//@Ignore
class ServiceAccountsTest extends SdkTest with ReadBehaviours {
  // TODO: We don't have permissions to list service accounts
//  "ServiceAccounts" should behave like readable(client.serviceAccounts)
}
