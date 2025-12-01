// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala

package object v1 {
  type OrError[T] = Either[Throwable, T]
}
