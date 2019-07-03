package com.cognite.sdk.scala

import com.cognite.sdk.scala.common.{CogniteId, Extractor, ExtractorInstances, Items, ItemsWithCursor}
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import io.circe.generic.auto._

package object v06 {
  implicit val sttpBackend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  implicit val extractor: Extractor[Data] = ExtractorInstances.dataExtractor

  implicit val assetsItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[Asset]]] =
    deriveDecoder[Data[ItemsWithCursor[Asset]]]
  implicit val assetsItemsDecoder: Decoder[Data[Items[Asset]]] =
    deriveDecoder[Data[Items[Asset]]]

  implicit val eventsItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[Event]]] =
    deriveDecoder[Data[ItemsWithCursor[Event]]]
  //implicit val eventsItemsDecoder: Decoder[Data[Items[Event]]] =
  //  deriveDecoder[Data[Items[Event]]]

  implicit val fileItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[File]]] =
    deriveDecoder[Data[ItemsWithCursor[File]]]
  implicit val fileItemsDecoder: Decoder[Data[Items[File]]] =
    deriveDecoder[Data[Items[File]]]

  implicit val timeSeriesItemsWithCursorDecoder: Decoder[Data[ItemsWithCursor[TimeSeries]]] =
    deriveDecoder[Data[ItemsWithCursor[TimeSeries]]]
  implicit val timeSeriesItemsDecoder: Decoder[Data[Items[TimeSeries]]] =
    deriveDecoder[Data[Items[TimeSeries]]]

  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder[CogniteId]
}
