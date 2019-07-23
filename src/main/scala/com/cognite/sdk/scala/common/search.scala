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

trait Search[R, Q, F[_]] extends WithRequestSession with BaseUri {
  lazy val searchUri = uri"$baseUri/search"
  def search(searchQuery: Q)(
      implicit sttpBackend: SttpBackend[F, _],
      itemsDecoder: Decoder[Items[R]],
      searchQueryEncoder: Encoder[Q]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, Items[R]]] =
      EitherDecoder.eitherDecoder[CdpApiError, Items[R]]
    requestSession
      .request
      .post(searchUri)
      .body(searchQuery)
      .response(asJson[Either[CdpApiError, Items[R]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => value.items
      }
      .send()
  }
}
