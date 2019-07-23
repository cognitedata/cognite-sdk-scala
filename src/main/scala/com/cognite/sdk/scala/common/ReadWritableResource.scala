package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait DeleteByIds[F[_], PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId]): F[Response[Unit]]
}

trait DeleteByExternalIds[F[_]] {
  def deleteByExternalIds(externalIds: Seq[String]): F[Response[Unit]]
}

trait Create[R, W, F[_]] extends WithRequestSession[F] with BaseUri {
  def createItems(items: Items[W]): F[Response[Seq[R]]]

  def create[T](items: Seq[T])(
      implicit t: Transformer[T, W]
  ): F[Response[Seq[R]]] =
    createItems(Items(items.map(_.transformInto[W])))
}

object Create {
  def createItems[F[_], R, W](requestSession: RequestSession[F], baseUri: Uri, items: Items[W])(
      implicit readDecoder: Decoder[ItemsWithCursor[R]],
      itemsEncoder: Encoder[Items[W]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsWithCursorDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    requestSession
      .send { request =>
        request
          .post(baseUri)
          .body(items)
          .response(asJson[Either[CdpApiError, ItemsWithCursor[R]]])
          .mapResponse {
            case Left(value) =>
              throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) => value.items
          }
      }
  }
}
