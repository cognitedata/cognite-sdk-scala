package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

trait DeleteByIds[F[_], PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId]): F[Unit]

  def deleteById(id: PrimitiveId): F[Unit] = deleteByIds(Seq(id))
}

trait DeleteByIdsWithIgnoreUnknownIds[F[_], PrimitiveId] extends DeleteByIds[F, PrimitiveId] {
  def deleteByIds(ids: Seq[PrimitiveId], ignoreUnknownIds: Boolean = false): F[Unit]

  def deleteById(id: PrimitiveId, ignoreUnknownIds: Boolean = false): F[Unit] =
    deleteByIds(Seq(id), ignoreUnknownIds)
}

object DeleteByIds {
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
          .body(Items(ids.map(CogniteInternalId)))
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }
  }

  def deleteByIdsWithIgnoreUnknownIds[F[_]](
      requestSession: RequestSession[F],
      baseUri: Uri,
      ids: Seq[Long],
      ignoreUnknownIds: Boolean
  ): F[Unit] = {
    implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
      EitherDecoder.eitherDecoder[CdpApiError, Unit]
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(ItemsWithIgnoreUnknownIds(ids.map(CogniteInternalId), ignoreUnknownIds))
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

trait DeleteByExternalIdsWithIgnoreUnknownIds[F[_]] extends DeleteByExternalIds[F] {
  def deleteByExternalIds(externalIds: Seq[String], ignoreUnknownIds: Boolean = false): F[Unit]

  def deleteByExternalId(externalId: String, ignoreUnknownIds: Boolean = false): F[Unit] =
    deleteByExternalIds(Seq(externalId), ignoreUnknownIds)
}

object DeleteByExternalIds {
  implicit val errorOrUnitDecoder: Decoder[Either[CdpApiError, Unit]] =
    EitherDecoder.eitherDecoder[CdpApiError, Unit]

  def deleteByExternalIdsWithIgnoreUnknownIds[F[_]](
      requestSession: RequestSession[F],
      baseUri: Uri,
      externalIds: Seq[String],
      ignoreUnknownIds: Boolean
  ): F[Unit] =
    // TODO: group deletes by max deletion request size
    //       or assert that length of `ids` is less than max deletion request size
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/delete")
          .body(
            ItemsWithIgnoreUnknownIds(externalIds.map(CogniteExternalId), ignoreUnknownIds)
          )
          .response(asJson[Either[CdpApiError, Unit]])
          .mapResponse {
            case Left(value) => throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/delete")
            case Right(Right(_)) => ()
          }
      }

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

trait Create[R, W, F[_]] extends WithRequestSession[F] with CreateOne[R, W, F] with BaseUri {
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

trait CreateOne[R, W, F[_]] extends WithRequestSession[F] with BaseUri {
  def createOne(item: W): F[R]

  def createOneFromRead(item: R)(
      implicit t: Transformer[R, W]
  ): F[R] = createOne(item.transformInto[W])

}

object CreateOne {
  def createOne[F[_], R, W](requestSession: RequestSession[F], baseUri: Uri, item: W)(
      implicit readDecoder: Decoder[R],
      itemsEncoder: Encoder[W]
  ): F[R] = {
    implicit val errorOrItemsWithCursorDecoder: Decoder[Either[CdpApiError, R]] =
      EitherDecoder.eitherDecoder[CdpApiError, R]
    requestSession
      .sendCdf { request =>
        request
          .post(baseUri)
          .body(item)
          .response(asJson[Either[CdpApiError, R]])
          .mapResponse {
            case Left(value) =>
              throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
            case Right(Right(value)) => value
          }
      }
  }
}
