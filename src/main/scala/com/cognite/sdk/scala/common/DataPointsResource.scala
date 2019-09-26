package com.cognite.sdk.scala.common

trait DataPointsResource[F[_]] {
  def insertById(id: Long, dataPoints: Seq[DataPoint]): F[Unit]
  def insertByExternalId(externalId: String, dataPoints: Seq[DataPoint]): F[Unit]
  def insertStringsById(id: Long, dataPoints: Seq[StringDataPoint]): F[Unit]
  def insertStringsByExternalId(id: String, dataPoints: Seq[StringDataPoint]): F[Unit]
  def deleteRangeById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Unit]
  def deleteRangeByExternalId(externalId: String, inclusiveStart: Long, exclusiveEnd: Long): F[Unit]
  def queryById(id: Long, inclusiveStart: Long, exclusiveEnd: Long): F[Seq[DataPoint]]
  def queryByExternalId(externalId: String, inclusiveStart: Long, exclusiveEnd: Long): F[Seq[DataPoint]]
  def queryStringsById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Seq[StringDataPoint]]
  def queryStringsByExternalId(
      externalId: String,
      inclusiveStart: Long,
      exclusiveEnd: Long
  ): F[Seq[StringDataPoint]]
  def getLatestDataPointById(id: Long): F[Option[DataPoint]]
  def getLatestDataPointByExternalId(externalId: String): F[Option[DataPoint]]
  def getLatestStringDataPointById(id: Long): F[Option[StringDataPoint]]
  def getLatestStringDataPointByExternalId(externalId: String): F[Option[StringDataPoint]]
  def queryAggregatesById(
      id: Long,
      inclusiveStart: Long,
      exclusiveEnd: Long,
      granularity: String,
      aggregateFunctions: Seq[String]
  ): F[Map[String, Seq[DataPoint]]]
  def queryAggregatesByExternalId(
      externalId: String,
      inclusiveStart: Long,
      exclusiveStart: Long,
      granularity: String,
      aggregateFunctions: Seq[String]
  ): F[Map[String, Seq[DataPoint]]]
}
