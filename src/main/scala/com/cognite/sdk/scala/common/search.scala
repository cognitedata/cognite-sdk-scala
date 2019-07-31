package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

trait SearchQuery[F, S] {
  val filter: Option[F]
  val search: Option[S]
  val limit: Int
}

trait Search[R, Q, F[_]] extends WithRequestSession[F] with BaseUri {
  def search(searchQuery: Q): F[Seq[R]]
}

object Search {
  def search[F[_], R, Q](requestSession: RequestSession[F], baseUri: Uri, searchQuery: Q)(
      implicit itemsDecoder: Decoder[Items[R]],
      searchQueryEncoder: Encoder[Q]
  ): F[Seq[R]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .sendCdf { request =>
        request
          .post(uri"$baseUri/search")
          .body(searchQuery)
          .response(asJson[Either[CdpApiError, Items[R]]])
          .mapResponse {
            case Left(value) =>
              throw value.error
            case Right(Left(cdpApiError)) => throw cdpApiError.asException(uri"$baseUri/search")
            case Right(Right(value)) => value.items
          }
      }
  }
}
