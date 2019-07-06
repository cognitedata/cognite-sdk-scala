package com.cognite.sdk.scala

import com.cognite.sdk.scala.common._
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

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
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] =
    deriveEncoder[AssetUpdate]
  implicit val updateAssetsItemsEncoder: Encoder[Items[AssetUpdate]] =
    deriveEncoder[Items[AssetUpdate]]
  implicit val assetsQueryEncoder: Encoder[AssetsQuery] =
    deriveEncoder[AssetsQuery]

  implicit val eventsItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[Event]]] =
    deriveDecoder[Id[ItemsWithCursor[Event]]]
  implicit val eventsItemsDecoder: Decoder[Id[Items[Event]]] =
    deriveDecoder[Id[Items[Event]]]
  implicit val createEventsItemsEncoder: Encoder[Items[CreateEvent]] =
    deriveEncoder[Items[CreateEvent]]
  implicit val eventUpdateEncoder: Encoder[EventUpdate] =
    deriveEncoder[EventUpdate]
  implicit val updateEventsItemsEncoder: Encoder[Items[EventUpdate]] =
    deriveEncoder[Items[EventUpdate]]
  implicit val eventsQueryEncoder: Encoder[EventsQuery] =
    deriveEncoder[EventsQuery]

  implicit val fileItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[File]]] =
    deriveDecoder[Id[ItemsWithCursor[File]]]
  implicit val fileItemsDecoder: Decoder[Id[Items[File]]] =
    deriveDecoder[Id[Items[File]]]
  implicit val createFileItemsEncoder: Encoder[Items[CreateFile]] =
    deriveEncoder[Items[CreateFile]]
  implicit val fileUpdateEncoder: Encoder[FileUpdate] =
    deriveEncoder[FileUpdate]
  implicit val updateFilesItemsEncoder: Encoder[Items[FileUpdate]] =
    deriveEncoder[Items[FileUpdate]]
  implicit val filesQueryEncoder: Encoder[FilesQuery] =
    deriveEncoder[FilesQuery]

  implicit val timeSeriesItemsWithCursorDecoder: Decoder[Id[ItemsWithCursor[TimeSeries]]] =
    deriveDecoder[Id[ItemsWithCursor[TimeSeries]]]
  implicit val timeSeriesItemsDecoder: Decoder[Id[Items[TimeSeries]]] =
    deriveDecoder[Id[Items[TimeSeries]]]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[CreateTimeSeries]] =
    deriveEncoder[Items[CreateTimeSeries]]
  implicit val timeSeriesQueryEncoder: Encoder[TimeSeriesQuery] =
    deriveEncoder[TimeSeriesQuery]

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

  implicit val cdpApiErrorDecoder: Decoder[CdpApiError] =
    deriveDecoder[CdpApiError]
  //implicit val cdpApiErrorUnitDecoder: Decoder[CdpApiError[Unit]] = deriveDecoder[CdpApiError[Unit]]
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder[Items[CogniteId]]

//  implicit def toOption[T: Manifest]: Transformer[T, Option[T]] =
//    (value: T) => Some(value)
}
