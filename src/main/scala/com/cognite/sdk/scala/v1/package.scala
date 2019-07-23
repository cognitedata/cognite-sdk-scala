package com.cognite.sdk.scala

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.resources.ThreeDAssetMapping
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

package object v1 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  // this is unfortunately necessary if we don't want to force users to import
  // io.circe.generic.auto._ themselves, due to the derivation of
  // errorOrItemsDecoder in Readable and other places.
  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val assetsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Asset]] =
    deriveDecoder[ItemsWithCursor[Asset]]
  implicit val assetsItemsDecoder: Decoder[Items[Asset]] =
    deriveDecoder[Items[Asset]]
  implicit val createAssetEncoder: Encoder[CreateAsset] = deriveEncoder[CreateAsset]
  implicit val createAssetsItemsEncoder: Encoder[Items[CreateAsset]] =
    deriveEncoder[Items[CreateAsset]]
  implicit val assetUpdateEncoder: Encoder[AssetUpdate] =
    deriveEncoder[AssetUpdate]
  implicit val updateAssetsItemsEncoder: Encoder[Items[AssetUpdate]] =
    deriveEncoder[Items[AssetUpdate]]
  implicit val timeRangeEncoder: Encoder[TimeRange] = deriveEncoder[TimeRange]
  implicit val assetsFilterEncoder: Encoder[AssetsFilter] =
    deriveEncoder[AssetsFilter]
  implicit val assetsSearchEncoder: Encoder[AssetsSearch] =
    deriveEncoder[AssetsSearch]
  implicit val assetsQueryEncoder: Encoder[AssetsQuery] =
    deriveEncoder[AssetsQuery]

  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]
  implicit val eventsItemsWithCursorDecoder: Decoder[ItemsWithCursor[Event]] =
    deriveDecoder[ItemsWithCursor[Event]]
  implicit val eventsItemsDecoder: Decoder[Items[Event]] =
    deriveDecoder[Items[Event]]
  implicit val createEventEncoder: Encoder[CreateEvent] = deriveEncoder[CreateEvent]
  implicit val createEventsItemsEncoder: Encoder[Items[CreateEvent]] =
    deriveEncoder[Items[CreateEvent]]
  implicit val eventUpdateEncoder: Encoder[EventUpdate] =
    deriveEncoder[EventUpdate]
  implicit val updateEventsItemsEncoder: Encoder[Items[EventUpdate]] =
    deriveEncoder[Items[EventUpdate]]
  implicit val eventsFilterEncoder: Encoder[EventsFilter] =
    deriveEncoder[EventsFilter]
  implicit val eventsSearchEncoder: Encoder[EventsSearch] =
    deriveEncoder[EventsSearch]
  implicit val eventsQueryEncoder: Encoder[EventsQuery] =
    deriveEncoder[EventsQuery]

  implicit val fileItemsWithCursorDecoder: Decoder[ItemsWithCursor[File]] =
    deriveDecoder[ItemsWithCursor[File]]
  implicit val fileDecoder: Decoder[File] = deriveDecoder[File]
  implicit val fileItemsDecoder: Decoder[Items[File]] =
    deriveDecoder[Items[File]]
  implicit val createFileEncoder: Encoder[CreateFile] =
    deriveEncoder[CreateFile]
  implicit val createFileItemsEncoder: Encoder[Items[CreateFile]] =
    deriveEncoder[Items[CreateFile]]
  implicit val fileUpdateEncoder: Encoder[FileUpdate] =
    deriveEncoder[FileUpdate]
  implicit val updateFilesItemsEncoder: Encoder[Items[FileUpdate]] =
    deriveEncoder[Items[FileUpdate]]
  implicit val filesFilterEncoder: Encoder[FilesFilter] =
    deriveEncoder[FilesFilter]
  implicit val filesSearchEncoder: Encoder[FilesSearch] =
    deriveEncoder[FilesSearch]
  implicit val filesQueryEncoder: Encoder[FilesQuery] =
    deriveEncoder[FilesQuery]

  implicit val timeSeriesDecoder: Decoder[TimeSeries] = deriveDecoder[TimeSeries]
  implicit val timeSeriesItemsWithCursorDecoder: Decoder[ItemsWithCursor[TimeSeries]] =
    deriveDecoder[ItemsWithCursor[TimeSeries]]
  implicit val timeSeriesItemsDecoder: Decoder[Items[TimeSeries]] =
    deriveDecoder[Items[TimeSeries]]
  implicit val createTimeSeriesEncoder: Encoder[CreateTimeSeries] = deriveEncoder[CreateTimeSeries]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[CreateTimeSeries]] =
    deriveEncoder[Items[CreateTimeSeries]]
  implicit val timeSeriesFilterEncoder: Encoder[TimeSeriesFilter] =
    deriveEncoder[TimeSeriesFilter]
  implicit val timeSeriesSearchEncoder: Encoder[TimeSeriesSearch] =
    deriveEncoder[TimeSeriesSearch]
  implicit val timeSeriesQueryEncoder: Encoder[TimeSeriesQuery] =
    deriveEncoder[TimeSeriesQuery]

  implicit val rawDatabaseItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawDatabase]] =
    deriveDecoder[ItemsWithCursor[RawDatabase]]
  implicit val rawDatabaseItemsDecoder: Decoder[Items[RawDatabase]] =
    deriveDecoder[Items[RawDatabase]]
  implicit val rawDatabaseItemsEncoder: Encoder[Items[RawDatabase]] =
    deriveEncoder[Items[RawDatabase]]
  implicit val rawDatabaseEncoder: Encoder[RawDatabase] = deriveEncoder[RawDatabase]
  implicit val rawDatabaseDecoder: Decoder[RawDatabase] = deriveDecoder[RawDatabase]

  implicit val rawTableItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawTable]] =
    deriveDecoder[ItemsWithCursor[RawTable]]
  implicit val rawTableItemsDecoder: Decoder[Items[RawTable]] =
    deriveDecoder[Items[RawTable]]
  implicit val rawTableItemsEncoder: Encoder[Items[RawTable]] =
    deriveEncoder[Items[RawTable]]
  implicit val rawTableEncoder: Encoder[RawTable] = deriveEncoder[RawTable]
  implicit val rawTableDecoder: Decoder[RawTable] = deriveDecoder[RawTable]

  implicit val rawRowEncoder: Encoder[RawRow] = deriveEncoder[RawRow]
  implicit val rawRowDecoder: Decoder[RawRow] = deriveDecoder[RawRow]
  implicit val rawRowItemsWithCursorDecoder: Decoder[ItemsWithCursor[RawRow]] =
    deriveDecoder[ItemsWithCursor[RawRow]]
  implicit val rawRowItemsDecoder: Decoder[Items[RawRow]] =
    deriveDecoder[Items[RawRow]]
  implicit val rawRowItemsEncoder: Encoder[Items[RawRow]] =
    deriveEncoder[Items[RawRow]]

  implicit val rawRowKeyEncoder: Encoder[RawRowKey] = deriveEncoder[RawRowKey]
  implicit val rawRowKeyItemsEncoder: Encoder[Items[RawRowKey]] =
    deriveEncoder[Items[RawRowKey]]

  implicit val threeDModelDecoder: Decoder[ThreeDModel] = deriveDecoder[ThreeDModel]
  implicit val threeDModelItemsDecoder: Decoder[Items[ThreeDModel]] = deriveDecoder[Items[ThreeDModel]]
  implicit val threeDModelItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDModel]] = deriveDecoder[ItemsWithCursor[ThreeDModel]]
  implicit val createThreeDModelDecoder: Decoder[CreateThreeDModel] = deriveDecoder[CreateThreeDModel]
  implicit val createThreeDModelEncoder: Encoder[CreateThreeDModel] = deriveEncoder[CreateThreeDModel]
  implicit val createThreeDModelItemsEncoder: Encoder[Items[CreateThreeDModel]] =
    deriveEncoder[Items[CreateThreeDModel]]

  implicit val threeDRevisionCameraDecoder: Decoder[Camera] = deriveDecoder[Camera]
  implicit val threeDRevisionCameraEncoder: Encoder[Camera] = deriveEncoder[Camera]
  implicit val threeDRevisionDecoder: Decoder[ThreeDRevision] = deriveDecoder[ThreeDRevision]
  implicit val threeDRevisionItemsDecoder: Decoder[Items[ThreeDRevision]] = deriveDecoder[Items[ThreeDRevision]]
  implicit val threeDRevisionItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDRevision]] =
    deriveDecoder[ItemsWithCursor[ThreeDRevision]]
  implicit val createThreeDRevisionDecoder: Decoder[CreateThreeDRevision] =
    deriveDecoder[CreateThreeDRevision]
  implicit val createThreeDRevisionEncoder: Encoder[CreateThreeDRevision] =
    deriveEncoder[CreateThreeDRevision]
  implicit val createThreeDRevisionItemsEncoder: Encoder[Items[CreateThreeDRevision]] =
    deriveEncoder[Items[CreateThreeDRevision]]

  implicit val threeDAssetMappingDecoder: Decoder[ThreeDAssetMapping] = deriveDecoder[ThreeDAssetMapping]
  implicit val threeDAssetMappingItemsWithCursorDecoder: Decoder[ItemsWithCursor[ThreeDAssetMapping]] =
    deriveDecoder[ItemsWithCursor[ThreeDAssetMapping]]

  implicit val cdpApiErrorPayloadDecoder: Decoder[CdpApiErrorPayload] =
    deriveDecoder[CdpApiErrorPayload]
  implicit val cdpApiErrorDecoder: Decoder[CdpApiError] =
    deriveDecoder[CdpApiError]
  //implicit val cdpApiErrorUnitDecoder: Decoder[CdpApiError[Unit]] = deriveDecoder[CdpApiError[Unit]]
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder[CogniteId]
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder[CogniteExternalId]
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder[Items[CogniteId]]
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder[Items[CogniteExternalId]]
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

//  implicit def toOption[T: Manifest]: Transformer[T, Option[T]] =
//    (value: T) => Some(value)
}
