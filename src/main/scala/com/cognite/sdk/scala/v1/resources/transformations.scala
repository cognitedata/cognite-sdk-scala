// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.client3._

class Transformations[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Create[TransformationRead, TransformationCreate, F]
    with RetrieveByIdsWithIgnoreUnknownIds[TransformationRead, F]
    with Readable[TransformationRead, F]
    with RetrieveByExternalIdsWithIgnoreUnknownIds[TransformationRead, F]
    with PartitionedFilter[TransformationRead, TransformationsFilter, F]
    with DeleteByCogniteIds[F] {
  import Transformations._
  override val baseUrl = uri"${requestSession.baseUrl}/transformations"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[TransformationRead]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      cursor,
      limit,
      partition,
      1000
    )

  override def retrieveByIds(
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationRead]] =
    RetrieveByIdsWithIgnoreUnknownIds.retrieveByIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  override def retrieveByExternalIds(
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Seq[TransformationRead]] =
    RetrieveByExternalIdsWithIgnoreUnknownIds.retrieveByExternalIds(
      requestSession,
      baseUrl,
      externalIds,
      ignoreUnknownIds
    )

  override def createItems(items: Items[TransformationCreate]): F[Seq[TransformationRead]] =
    Create.createItems[F, TransformationRead, TransformationCreate](
      requestSession,
      baseUrl,
      items
    )

  override def delete(ids: Seq[CogniteId], ignoreUnknownIds: Boolean = false): F[Unit] =
    DeleteByCogniteIds.deleteWithIgnoreUnknownIds(
      requestSession,
      baseUrl,
      ids,
      ignoreUnknownIds
    )

  def filter(
      filter: TransformationsFilter,
      limit: Option[Int],
      aggregatedProperties: Option[Seq[String]]
  ): fs2.Stream[F, TransformationRead] =
    filterWithNextCursor(filter, None, limit, aggregatedProperties)

  def filterPartitions(
      filter: TransformationsFilter,
      numPartitions: Int,
      limitPerPartition: Option[Int],
      aggregatedProperties: Option[Seq[String]]
  ): Seq[fs2.Stream[F, TransformationRead]] =
    1.to(numPartitions).map { i =>
      Readable
        .pullFromCursor(
          None,
          limitPerPartition,
          Some(Partition(i, numPartitions)),
          filterWithCursor(filter, _, _, _, aggregatedProperties)
        )
        .stream
    }

  private[sdk] def filterWithCursor(
      filter: TransformationsFilter,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[TransformationRead]] =
    Filter.filterWithCursor(
      requestSession,
      uri"$baseUrl/filter",
      filter,
      cursor,
      limit,
      partition,
      Constants.defaultBatchSize,
      aggregatedProperties
    )
}

object Transformations {
  implicit val generalDataSourceDecoder: Decoder[GeneralDataSource] =
    deriveDecoder[GeneralDataSource]
  implicit val rawDataSourceDecoder: Decoder[RawDataSource] = deriveDecoder[RawDataSource]
  implicit val seqRowDataSourceDecoder: Decoder[SequenceRowDataSource] =
    deriveDecoder[SequenceRowDataSource]

  implicit val destinationDataSourceDecoder: Decoder[DestinationDataSource] =
    List[Decoder[DestinationDataSource]](
      Decoder[GeneralDataSource].widen,
      Decoder[RawDataSource].widen,
      Decoder[SequenceRowDataSource].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[GeneralDataSource].widen)

  implicit val jobDetailDecoder: Decoder[JobDetails] = deriveDecoder[JobDetails]

  implicit val readDecoder: Decoder[TransformationRead] = deriveDecoder[TransformationRead]
  implicit val readItemsWithCursorDecoder: Decoder[ItemsWithCursor[TransformationRead]] =
    deriveDecoder[ItemsWithCursor[TransformationRead]]
  implicit val readItemsDecoder: Decoder[Items[TransformationRead]] =
    deriveDecoder[Items[TransformationRead]]

  implicit val generalDataSourceEncoder: Encoder[GeneralDataSource] =
    deriveEncoder[GeneralDataSource]
  implicit val rawDataSourceEncoder: Encoder[RawDataSource] = deriveEncoder[RawDataSource]
  implicit val sequenceRowDataSourceEncoder: Encoder[SequenceRowDataSource] =
    deriveEncoder[SequenceRowDataSource]

  implicit val destinationDataSourceEncoder: Encoder[DestinationDataSource] = Encoder.instance {
    case g: GeneralDataSource => generalDataSourceEncoder(g)
    case r: RawDataSource => rawDataSourceEncoder(r)
    case sr: SequenceRowDataSource => sequenceRowDataSourceEncoder(sr)
  }

  import FlatOidcCredentials.credentialEncoder

  implicit val createEncoder: Encoder[TransformationCreate] = deriveEncoder[TransformationCreate]
  implicit val createItemsEncoder: Encoder[Items[TransformationCreate]] =
    deriveEncoder[Items[TransformationCreate]]

  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]
  implicit val deleteRequestWithRecursiveAndIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithRecursiveAndIgnoreUnknownIds] =
    deriveEncoder[ItemsWithRecursiveAndIgnoreUnknownIds]
  implicit val cogniteExternalIdDecoder: Decoder[CogniteExternalId] =
    deriveDecoder[CogniteExternalId]

  implicit val transformationsFilterRequestEncoder: Encoder[FilterRequest[TransformationsFilter]] =
    deriveEncoder[FilterRequest[TransformationsFilter]]
}
