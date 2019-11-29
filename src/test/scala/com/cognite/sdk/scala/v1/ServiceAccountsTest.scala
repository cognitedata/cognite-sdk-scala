package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTest}
import org.scalatest.Ignore

// TODO: We don't have permissions to list service accounts
@Ignore
class ServiceAccountsTest extends SdkTest with ReadBehaviours {
  "ServiceAccounts" should behave like readable(client.serviceAccounts)
}
