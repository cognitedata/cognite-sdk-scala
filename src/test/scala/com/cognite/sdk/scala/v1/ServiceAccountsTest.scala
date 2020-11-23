// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.{ReadBehaviours, SdkTestSpec}
// FIXME: Not sure why @Ignore isn't working. "behave like" doesn't seem to support ignore.
//import org.scalatest.Ignore
//
//@Ignore
class ServiceAccountsTest extends SdkTestSpec with ReadBehaviours {
  // TODO: We don't have permissions to list service accounts
//  "ServiceAccounts" should behave like readable(client.serviceAccounts)
}
