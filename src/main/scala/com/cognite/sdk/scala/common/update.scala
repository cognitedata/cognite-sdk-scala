package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

final case class UpdateRequest(update: Json, id: Long)

trait Update[R <: WithId[Long], U <: WithId[Long], F[_]] extends WithRequestSession with BaseUri {
  lazy val updateUri = uri"$baseUri/update"

  def updateItems(updates: Seq[U])(
      implicit sttpBackend: SttpBackend[F, _],
      updateEncoder: Encoder[U],
      itemsDecoder: Decoder[Items[R]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    implicit val _: Encoder[UpdateRequest] = deriveEncoder[UpdateRequest]
    implicit val updateRequestItemsEncoder: Encoder[Items[UpdateRequest]] = deriveEncoder[Items[UpdateRequest]]
    require(updates.forall(_.id > 0), "Update requires an id to be set")
    implicit val printer: Printer =
      Printer(dropNullValues = true, indent = "", preserveOrder = false)
    requestSession
      .request
      .post(updateUri)
      .body(Items(updates.map { update =>
        UpdateRequest(update.asJson.mapObject(_.remove("id")), update.id)
      }))
      .response(asJson[Either[CdpApiError, Items[R]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => value.items
      }
      .send()
  }

  // scalastyle: off
  def update[T](items: Seq[T])(
      implicit sttpBackend: SttpBackend[F, _],
      updateEncoder: Encoder[U],
      itemsDecoder: Decoder[Items[R]],
      t: Transformer[T, U]
  ): F[Response[Seq[R]]] =
    updateItems(items.map(_.transformInto[U]))
}
