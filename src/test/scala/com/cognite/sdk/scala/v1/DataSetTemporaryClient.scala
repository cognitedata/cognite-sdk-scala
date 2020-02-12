package com.cognite.sdk.scala.v1

import java.time._

import cats.Monad
import com.cognite.sdk.scala.common._
import io.circe.{Decoder, Encoder}
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class DataSetCreate(
  name: Option[String],
  description: Option[String] = None,
  externalId: Option[String] = None,
  metadata: Map[String, String] = Map(),
  writeProtected: Boolean = false
)

case class DataSetFilter(
  externalIdPrefix: Option[String] = None,
  metadata: Option[Map[String, String]] = None,
  writeProtected: Option[Boolean] = None,
  createdTime: Option[TimeRange] = None,
  lastUpdatedTime: Option[TimeRange] = None
)

case class DataSet(
  name: Option[String],
  writeProtected: Boolean,
  description: Option[String],
  metadata: Map[String, String],
  id: Long,
  createdTime: Instant,
  lastUpdatedTime: Instant
)

case class DataSetListQuery(
  filter: DataSetFilter,
  limit: Int
)

object DataSetTemporaryClient {

  implicit val dataSetFilterEncoder: Encoder[DataSetFilter] = deriveEncoder
  implicit val dataSetDecoder: Decoder[DataSet] = deriveDecoder
  implicit val dataSetItemsDecoder: Decoder[Items[DataSet]] = deriveDecoder
  implicit val dataSetCreateEncoder: Encoder[DataSetCreate] = deriveEncoder
  implicit val dataSetCreateItemsEncoder: Encoder[Items[DataSetCreate]] = deriveEncoder
  implicit val dataSetListQueryEncoder: Encoder[DataSetListQuery] = deriveEncoder

  def createDataSet[F[_]: Monad, S](
    c: GenericClient[F, S],
    dataSetCreate: DataSetCreate
  ): F[DataSet] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[DataSet]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[DataSet]]
    val playgroundBaseUrl = c.requestSession.baseUrl.toString()//.replaceFirst("/v1/", "/playground/")
    c.requestSession.post[DataSet, Items[DataSet], Items[DataSetCreate]](
      Items(Seq(dataSetCreate)),
      uri"$playgroundBaseUrl/datasets",
      value => value.items.head
    )
  }

  def listDataSets[F[_]: Monad, S](
    c: GenericClient[F, S],
    query: DataSetFilter,
    limit: Int = 100
  ): F[Seq[DataSet]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[DataSet]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[DataSet]]
    val playgroundBaseUrl = c.requestSession.baseUrl.toString()//.replaceFirst("/v1/", "/playground/")
    c.requestSession.post[Seq[DataSet], Items[DataSet], DataSetListQuery](
      DataSetListQuery(query, limit),
      uri"$playgroundBaseUrl/datasets/list",
      value => value.items
    )
  }
}
