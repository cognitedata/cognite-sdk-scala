package com.cognite.sdk.scala.common

trait DataPointsResource[F[_], I] {
  def insertById(id: I, dataPoints: Seq[DataPoint]): F[Unit]
  def insertStringsById(id: I, dataPoints: Seq[StringDataPoint]): F[Unit]
  def deleteRangeById(id: I, inclusiveStart: Long, exclusiveEnd: Long): F[Unit]
  def queryById(id: I, inclusiveStart: Long, exclusiveEnd: Long): F[Seq[DataPoint]]
  def queryStringsById(
      id: I,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Seq[StringDataPoint]]
  def getLatestDataPointById(id: I): F[Option[DataPoint]]
  def getLatestStringDataPointById(id: I): F[Option[StringDataPoint]]
}
