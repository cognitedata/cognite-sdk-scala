package com.cognite.sdk.scala

import com.cognite.sdk.scala.common.{CdpApiError, CogniteId, Extractor, Items, ItemsWithCursor}
import com.cognite.sdk.scala.v06.resources.{
  Asset,
  CreateAsset,
  CreateEvent,
  CreateTimeSeries,
  Event,
  File,
  TimeSeries
}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.generic.auto._

package object v06 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val extractor: Extractor[Data] = new Extractor[Data] {
    override def extract[A](c: Data[A]): A = c.data
  }

  // this is unfortunately necessary if we don't want to force users to import
  // io.circe.generic.auto._ themselves, due to the derivation of
  // errorOrItemsDecoder in Readable and other places.
  implicit val longItemsEncoder: Encoder[Items[Long]] = deriveEncoder[Items[Long]]
  implicit val assetsItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[Asset]]] =
    deriveDecoder[Data[ItemsWithCursor[Asset]]]
  implicit val assetsItemsDecoder: Decoder[Data[Items[Asset]]] =
    deriveDecoder[Data[Items[Asset]]]
  implicit val createAssetsItemsEncoder: Encoder[Items[CreateAsset]] =
    deriveEncoder[Items[CreateAsset]]

  implicit val eventsItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[Event]]] =
    deriveDecoder[Data[ItemsWithCursor[Event]]]
  implicit val eventsItemsDecoder: Decoder[Data[Items[Event]]] =
    deriveDecoder[Data[Items[Event]]]
  implicit val createEventsItemsEncoder: Encoder[Items[CreateEvent]] =
    deriveEncoder[Items[CreateEvent]]

  implicit val fileItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[File]]] =
    deriveDecoder[Data[ItemsWithCursor[File]]]
  implicit val fileItemsDecoder: Decoder[Data[Items[File]]] =
    deriveDecoder[Data[Items[File]]]
//  implicit val createFileItemsEncoder: Encoder[Items[CreateFile]] =
//    deriveEncoder[Items[CreateFile]]

  implicit val timeSeriesItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[TimeSeries]]] =
    deriveDecoder[Data[ItemsWithCursor[TimeSeries]]]
  implicit val timeSeriesItemsDecoder: Decoder[Data[Items[TimeSeries]]] =
    deriveDecoder[Data[Items[TimeSeries]]]
  implicit val createTimeSeriesItemsEncoder: Encoder[Items[CreateTimeSeries]] =
    deriveEncoder[Items[CreateTimeSeries]]

  implicit val cdpApiErrorCogniteIdDecoder: Decoder[CdpApiError[CogniteId]] =
    deriveDecoder[CdpApiError[CogniteId]]
  implicit val cdpApiErrorUnitDecoder: Decoder[CdpApiError[Unit]] = deriveDecoder[CdpApiError[Unit]]
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder[CogniteId]
}
