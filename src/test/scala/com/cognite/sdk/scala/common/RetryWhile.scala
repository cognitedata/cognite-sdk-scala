package com.cognite.sdk.scala.common

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Random
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException

trait RetryWhile {
  def retryWithExpectedResult[A](
      action: => A,
      assertion: A => Assertion,
      retriesRemaining: Int = Constants.DefaultMaxRetries,
      initialDelay: FiniteDuration = Constants.DefaultInitialRetryDelay
  ): A = {
    val exponentialDelay = (Constants.DefaultMaxBackoffDelay / 2).min(initialDelay * 2)
    val randomDelayScale =
      (Constants.DefaultMaxBackoffDelay / 2).min(Constants.DefaultInitialRetryDelay * 2).toMillis
    val nextDelay = Random.nextInt(randomDelayScale.toInt).millis + exponentialDelay
    try {
      val result = action
      assertion(result)
      result
    } catch {
      case _: TestFailedException if (retriesRemaining > 0) => {
        Thread.sleep(initialDelay.toMillis)
        retryWithExpectedResult[A](action, assertion, retriesRemaining - 1, nextDelay)
      }
    }
  }
}
