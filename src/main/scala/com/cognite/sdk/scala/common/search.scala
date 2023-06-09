// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1._
import sttp.client3._
import sttp.client3.circe._
import io.circe.{Decoder, Encoder, Printer}
import sttp.model.Uri

trait SearchQuery[F, S] {
  val filter: Option[F]
  val search: Option[S]
  val limit: Int
}

trait Search[R, Q, F[_]] extends WithRequestSession[F] with BaseUrl {
  def search(searchQuery: Q): F[Seq[R]]
}

object Search {
  implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def search[F[_], R, Q](requestSession: RequestSession[F], baseUrl: Uri, searchQuery: Q)(
      implicit itemsDecoder: Decoder[Items[R]],
      searchQueryEncoder: Encoder[Q]
  ): F[Seq[R]] =
    requestSession
      .post[Seq[R], Items[R], Q](
        searchQuery,
        uri"$baseUrl/search",
        value => value.items
      )
}
