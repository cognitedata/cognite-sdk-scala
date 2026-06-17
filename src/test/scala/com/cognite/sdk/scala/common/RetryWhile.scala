// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Success, Try}
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException

import scala.annotation.tailrec

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
trait RetryWhile {
  @tailrec
  final def retryWithExpectedResult[A](
      action: => A,
      assertion: A => Assertion,
      retriesRemaining: Int = 8,
      initialDelay: FiniteDuration = Constants.DefaultInitialRetryDelay,
      retryOnException: Boolean = false,
      sleep: Long => Unit = delay => Thread.sleep(delay)
  ): A = {
    val currentDelay = Random.nextInt(initialDelay.toMillis.toInt)
    val nextDelay = Constants.DefaultMaxBackoffDelay.min(initialDelay * 2)
    Try {
      val result = action
      val _ = assertion(result)
      result
    } match {
      case Failure(_: TestFailedException) if retriesRemaining > 0 =>
        sleep(currentDelay.toLong)
        retryWithExpectedResult[A](action, assertion, retriesRemaining - 1, nextDelay, retryOnException, sleep)
      case Failure(_) if retriesRemaining > 0 && retryOnException =>
        sleep(currentDelay.toLong)
        retryWithExpectedResult[A](action, assertion, retriesRemaining - 1, nextDelay, retryOnException, sleep)
      case Failure(exception) => throw exception
      case Success(value) => value
    }
  }
}
