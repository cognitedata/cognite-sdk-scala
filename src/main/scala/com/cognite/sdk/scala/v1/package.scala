package com.cognite.sdk.scala

import com.cognite.sdk.scala.common.{CdpApiError, CogniteId, Extractor, Items, ItemsWithCursor}
import com.cognite.sdk.scala.v1.resources.{
  Asset,
  CreateAsset,
  CreateEvent,
  CreateFile,
  CreateTimeSeries,
  Event,
  File,
  RawDatabase,
  RawRow,
  RawRowKey,
  RawTable,
  TimeSeries
}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.generic.auto._

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val extractor: Extractor[Id] = new Extractor[Id] {
    override def extract[A](c: Id[A]): A = c
  }

  // this is unfortunately necessary if we don't want to force users to import
  // io.circe.generic.auto._ themselves, due to the derivation of
  // errorOrItemsDecoder in Readable and other places.
  implicit val assetsItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[Asset]]] =
    deriveDecoder[Id[ItemsWithCursor[Asset]]]
  implicit val assetsItemsDecoder: Decoder[Id[Items[Asset]]] =
    deriveDecoder[Id[Items[Asset]]]
  implicit val createAssetsItemsEncoder: Encoder[Items[CreateAsset]] =
    deriveEncoder[Items[CreateAsset]]

  implicit val eventsItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[Event]]] =
    deriveDecoder[Id[ItemsWithCursor[Event]]]
  implicit val eventsItemsDecoder: Decoder[Id[Items[Event]]] =
    deriveDecoder[Id[Items[Event]]]
  implicit val createEventsItemsEncoder: Encoder[Items[CreateEvent]] =
    deriveEncoder[Items[CreateEvent]]

  implicit val fileItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[File]]] =
    deriveDecoder[Id[ItemsWithCursor[File]]]
  implicit val fileItemsDecoder: Decoder[Id[Items[File]]] =
    deriveDecoder[Id[Items[File]]]
  implicit val createFileItemsEncoder: Encoder[Items[CreateFile]] =
    deriveEncoder[Items[CreateFile]]

  implicit val timeSeriesItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[TimeSeries]]] =
    deriveDecoder[Id[ItemsWithCursor[TimeSeries]]]
  implicit val timeSeriesItemsDecoder: Decoder[Id[Items[TimeSeries]]] =
    deriveDecoder[Id[Items[TimeSeries]]]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[CreateTimeSeries]] =
    deriveEncoder[Items[CreateTimeSeries]]

  implicit val rawDatabaseItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[RawDatabase]]] =
    deriveDecoder[Id[ItemsWithCursor[RawDatabase]]]
  implicit val rawDatabaseItemsDecoder: Decoder[Id[Items[RawDatabase]]] =
    deriveDecoder[Id[Items[RawDatabase]]]
  implicit val rawDatabaseItemsEncoder: Encoder[Items[RawDatabase]] =
    deriveEncoder[Items[RawDatabase]]

  implicit val rawTableItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[RawTable]]] =
    deriveDecoder[Id[ItemsWithCursor[RawTable]]]
  implicit val rawTableItemsDecoder: Decoder[Id[Items[RawTable]]] =
    deriveDecoder[Id[Items[RawTable]]]
  implicit val rawTableItemsEncoder: Encoder[Items[RawTable]] =
    deriveEncoder[Items[RawTable]]

  implicit val rawRowItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[RawRow]]] =
    deriveDecoder[Id[ItemsWithCursor[RawRow]]]
  implicit val rawRowItemsDecoder: Decoder[Id[Items[RawRow]]] =
    deriveDecoder[Id[Items[RawRow]]]
  implicit val rawRowItemsEncoder: Encoder[Items[RawRow]] =
    deriveEncoder[Items[RawRow]]

  implicit val rawRowKeyEncoder: Encoder[Items[RawRowKey]] =
    deriveEncoder[Items[RawRowKey]]

  implicit val cdpApiErrorCogniteIdDecoder: Decoder[CdpApiError[CogniteId]] =
    deriveDecoder[CdpApiError[CogniteId]]
  implicit val cdpApiErrorUnitDecoder: Decoder[CdpApiError[Unit]] = deriveDecoder[CdpApiError[Unit]]
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder[Items[CogniteId]]
}
