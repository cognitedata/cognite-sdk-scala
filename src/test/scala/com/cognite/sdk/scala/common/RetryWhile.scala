// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Success, Try}
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException

import scala.annotation.tailrec

trait RetryWhile {
  @tailrec
  @SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
  final def retryWithExpectedResult[A](
      action: => A,
      assertion: A => Assertion,
      retriesRemaining: Int = 5,
      initialDelay: FiniteDuration = Constants.DefaultInitialRetryDelay
  ): A = {
    val exponentialDelay = (Constants.DefaultMaxBackoffDelay / 2).min(initialDelay * 2)
    Try {
      val result = action
      val _ = assertion(result)
      result
    } match {
      case Failure(_: TestFailedException) if retriesRemaining > 0 =>
        Thread.sleep(initialDelay.toMillis + Random.nextInt(exponentialDelay.toMillis.toInt))
        retryWithExpectedResult[A](action, assertion, retriesRemaining - 1, exponentialDelay)
      case Failure(exception) => throw exception
      case Success(value) => value
    }
  }
}
