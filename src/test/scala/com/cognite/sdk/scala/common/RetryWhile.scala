// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException

import scala.annotation.tailrec

trait RetryWhile {
  @tailrec
  final def retryWithExpectedResult[A](
      action: => A,
      assertion: A => Assertion,
      retriesRemaining: Int = Constants.DefaultMaxRetries,
      initialDelay: FiniteDuration = Constants.DefaultInitialRetryDelay
  ): A = {
    val exponentialDelay = (Constants.DefaultMaxBackoffDelay / 2).min(initialDelay * 2)
    val randomDelayScale =
      (Constants.DefaultMaxBackoffDelay / 2).min(Constants.DefaultInitialRetryDelay * 2).toMillis
    val nextDelay = Random.nextInt(randomDelayScale.toInt).millis + exponentialDelay
    Try {
      val result = action
      val _ = assertion(result)
      result
    } match {
      case Failure(_: TestFailedException) if retriesRemaining > 0 =>
        Thread.sleep(initialDelay.toMillis)
        retryWithExpectedResult[A](action, assertion, retriesRemaining - 1, nextDelay)
      case Failure(exception) => throw exception
      case Success(value) => value
    }
  }
}
