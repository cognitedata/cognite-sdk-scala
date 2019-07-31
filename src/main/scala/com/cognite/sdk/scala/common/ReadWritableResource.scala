package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.derivation.deriveEncoder
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait DeleteByIds[F[_], PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId]): F[Unit]

  def deleteById(id: PrimitiveId): F[Unit] = deleteByIds(Seq(id))
}

object DeleteByIds {
  implicit val cogniteIdEncoder: Encoder[CogniteId] = deriveEncoder
  implicit val cogniteIdItemsEncoder: Encoder[Items[CogniteId]] = deriveEncoder

  def deleteByIds[F[_]](
      requestSession: RequestSession[F],
      baseUri: Uri,
      ids: Seq[Long]
  ): F[Unit] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(ids.map(CogniteId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }
}

trait DeleteByExternalIds[F[_]] {
  def deleteByExternalIds(externalIds: Seq[String]): F[Unit]

  def deleteByExternalId(externalId: String): F[Unit] = deleteByExternalIds(Seq(externalId))
}

object DeleteByExternalIds {
  implicit val cogniteExternalIdEncoder: Encoder[CogniteExternalId] = deriveEncoder
  implicit val cogniteExternalIdItemsEncoder: Encoder[Items[CogniteExternalId]] = deriveEncoder
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def deleteByExternalIds[F[_]](
      requestSession: RequestSession[F],
      baseUri: Uri,
      externalIds: Seq[String]
  ): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(Items(externalIds.map(CogniteExternalId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
}

trait Create[R, W, F[_]] extends WithRequestSession[F] with BaseUri {
  def createItems(items: Items[W]): F[Seq[R]]

  def create(items: Seq[W]): F[Seq[R]] =
    createItems(Items(items))

  def createFromRead(items: Seq[R])(
      implicit t: Transformer[R, W]
  ): F[Seq[R]] =
    createItems(Items(items.map(_.transformInto[W])))

  def createOne(item: W): F[R] =
    requestSession.map(
      create(Seq(item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when creating item")
        }
    )

  def createOneFromRead(item: R)(
      implicit t: Transformer[R, W]
  ): F[R] =
    requestSession.map(
      createFromRead(Seq(item)),
      (r1: Seq[R]) =>
        r1.headOption match {
          case Some(value) => value
          case None => throw SdkException("Unexpected empty response when creating item")
        }
    )
}

object Create {
  def createItems[F[_], R, W](requestSession: RequestSession[F], baseUri: Uri, items: Items[W])(
      implicit readDecoder: Decoder[ItemsWithCursor[R]],
      itemsEncoder: Encoder[Items[W]]
  ): F[Seq[R]] = {
    implicit val errorOrItemsWithCursorDecoder: Decoder[Either[CdpApiError, ItemsWithCursor[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, ItemsWithCursor[R]]
    requestSession
      .sendCdf { request =>
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
