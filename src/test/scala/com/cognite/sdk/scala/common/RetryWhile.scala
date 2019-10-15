package com.cognite.sdk.scala.common

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Random
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException

trait RetryWhile {
  def retryWithExpectedResult[A](
        action: => A,
        result: Option[A],
        shouldRetry: Seq[A => Assertion],
        retriesRemaining: Int = Constants.DefaultMaxRetries,
        initialDelay: FiniteDuration = Constants.DefaultInitialRetryDelay
      ): A = {
    val exponentialDelay = (Constants.DefaultMaxBackoffDelay / 2).min(initialDelay * 2)
    val randomDelayScale = (Constants.DefaultMaxBackoffDelay / 2).min(Constants.DefaultInitialRetryDelay * 2).toMillis
    val nextDelay = Random.nextInt(randomDelayScale.toInt).millis + exponentialDelay
    shouldRetry.foreach(assertion => {
        try {
          assertion(result.getOrElse(action))
        } catch {
          case testFailed: TestFailedException =>
          if (retriesRemaining > 0) {
            Thread.sleep(initialDelay.toMillis)
            val actionValue = action
            retryWithExpectedResult[A](action, Some(actionValue), Seq(assertion), retriesRemaining - 1, nextDelay)
          } else {
            throw testFailed
          }
        }
      })
    result.getOrElse(action)
  }
}
