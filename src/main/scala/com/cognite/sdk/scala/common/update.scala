package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

final case class UpdateRequest(update: Json, id: Long)

trait Update[R <: WithId[Long], U <: WithId[Long], F[_], C[_]] extends RequestSession with BaseUri {
  lazy val updateUri = uri"$baseUri/update"
  def updateItems(updates: Seq[U])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      updateEncoder: Encoder[U],
      items: Decoder[C[Items[R]]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError[CogniteId], C[Items[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError[CogniteId], C[Items[R]]]
    request
      .post(updateUri)
      .body(Items(updates.map { update =>
        UpdateRequest(ToUpdate(updateEncoder(update)), update.id)
      }))
      .response(asJson[Either[CdpApiError[CogniteId], C[Items[R]]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }

  // scalastyle: off
  def update[T](items: Seq[T])(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError[CogniteId]],
      updateEncoder: Encoder[U],
      itemsWithCursorDecoder: Decoder[C[Items[R]]],
      t: Transformer[T, U]
  ): F[Response[Seq[R]]] =
    updateItems(items.map(_.transformInto[U]))
}
