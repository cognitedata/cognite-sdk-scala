package com.cognite.sdk.scala.common
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object Constants {
  val dataPointsBatchSize: Int = 100000
  val aggregatesBatchSize: Int = 10000
  val defaultBatchSize: Int = 1000
  val rowsBatchSize: Int = 10000
  val DefaultMaxRetries: Int = 10
  val DefaultInitialRetryDelay: FiniteDuration = 150.millis
  val DefaultMaxBackoffDelay: FiniteDuration = 120.seconds
}
