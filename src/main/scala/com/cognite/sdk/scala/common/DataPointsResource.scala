package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Response

trait DataPointsResource[F[_]] {
  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Response[Unit]]
  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Response[Unit]]
  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]]
  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]]
  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Response[Seq[StringDataPoint]]]
  def getLatestById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Response[DataPoint]]
}
