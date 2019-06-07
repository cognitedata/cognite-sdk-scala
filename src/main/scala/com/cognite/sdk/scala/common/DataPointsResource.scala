package com.cognite.sdk.scala.common

import com.softwaremill.sttp.Response

trait DataPointsResource[F[_], I] {
  def insertById(id: I, dataPoints: Seq[DataPoint]): F[Response[Unit]]
  def insertStringsById(id: I, dataPoints: Seq[StringDataPoint]): F[Response[Unit]]
  def deleteRangeById(id: I, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Unit]]
  def queryById(id: I, inclusiveStart: Long, exclusiveEnd: Long): F[Response[Seq[DataPoint]]]
  def queryStringsById(
      id: I,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Response[Seq[StringDataPoint]]]
  def getLatestDataPointById(id: I): F[Response[Option[DataPoint]]]
  def getLatestStringDataPointById(id: I): F[Response[Option[StringDataPoint]]]
}
