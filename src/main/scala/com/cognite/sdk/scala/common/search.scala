package com.cognite.sdk.scala.common

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.{Decoder, Encoder}

trait SearchQuery[F, S] {
  val filter: Option[F]
  val search: Option[S]
  val limit: Int
}

trait Search[R, Q, F[_], C[_]] extends RequestSession with BaseUri {
  lazy val searchUri = uri"$baseUri/search"
  def search(searchQuery: Q)(
      implicit sttpBackend: SttpBackend[F, _],
      auth: Auth,
      extractor: Extractor[C],
      errorDecoder: Decoder[CdpApiError],
      searchEncoder: Encoder[Q],
      items: Decoder[C[Items[R]]]
  ): F[Response[Seq[R]]] = {
    implicit val errorOrItemsDecoder: Decoder[Either[CdpApiError, C[Items[R]]]] =
      EitherDecoder.eitherDecoder[CdpApiError, C[Items[R]]]
    request
      .post(searchUri)
      .body(searchQuery)
      .response(asJson[Either[CdpApiError, C[Items[R]]]])
      .mapResponse {
        case Left(value) =>
          throw value.error
        case Right(Left(cdpApiError)) => throw cdpApiError.asException(baseUri)
        case Right(Right(value)) => extractor.extract(value).items
      }
      .send()
  }
}
