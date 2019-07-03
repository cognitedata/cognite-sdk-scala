package com.cognite.sdk.scala.v06.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v06.Data
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

final case class Event(
    id: Long = 0,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None,
    createdTime: Long = 0,
    lastUpdatedTime: Long = 0
) extends WithId[Long]

final case class CreateEvent(
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    description: Option[String] = None,
    `type`: Option[String] = None,
    subtype: Option[String] = None,
    metadata: Option[Map[String, String]] = None,
    assetIds: Option[Seq[Long]] = None,
    source: Option[String] = None,
    sourceId: Option[String] = None
)

class Events[F[_]](project: String)(implicit auth: Auth)
    extends ReadWritableResourceV0_6[Event, CreateEvent, F]
    with ResourceV0_6[F] {
  override val baseUri = uri"https://api.cognitedata.com/api/0.6/projects/$project/events"

  override def deleteByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsEncoder: Encoder[Items[Long]]
  ): F[Response[Unit]] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError[Long], Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError[Long], Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    request
      .post(uri"$baseUri/delete")
      .body(Items(ids))
      .response(asJson[Either[CdpApiError[Long], Unit]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
        case Right(Right(_)) => ()
      }
      .send()
  }

  // 0.6 byids for events uses CogniteId in the request body
  override def retrieveByIds(ids: Seq[Long])(
      implicit sttpBackend: SttpBackend[F, _],
      extractor: Extractor[Data],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      itemsDecoder: Decoder[Data[Items[Event]]],
      internalIdItemsEncoder: Encoder[Items[Long]]
  ): F[Response[Seq[Event]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError[CogniteId], Data[Items[Event]]]] =
      EitherDecoder.eitherDecoder[CdpApiError[CogniteId], Data[Items[Event]]]
    request
      .get(uri"$baseUri/byids")
      .body(Items(ids.map(CogniteId)))
      .response(asJson[Either[CdpApiError[CogniteId], Data[Items[Event]]]])
      .mapResponse {
        case Left(value) => throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/byids")
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }
}
