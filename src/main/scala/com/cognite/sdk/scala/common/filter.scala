// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import com.cognite.sdk.scala.v1.RequestSession
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import sttp.client3._
import sttp.client3.jsoniter_scala._
import sttp.model._

final case class FilterRequest[T](
    filter: T,
    limit: Option[Int],
    cursor: Option[String],
    partition: Option[String],
    aggregatedProperties: Option[Seq[String]]
)

trait Filter[R, Fi, F[_]] extends WithRequestSession[F] with BaseUrl {
  private[sdk] def filterWithCursor(
      filter: Fi,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      aggregatedProperties: Option[Seq[String]] = None
  ): F[ItemsWithCursor[R]]
}

object Filter {
  // scalastyle:off parameter.number
  def filterWithCursor[F[_], R, Fi](
      requestSession: RequestSession[F],
      baseUrl: Uri,
      filter: Fi,
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition],
      batchSize: Int,
      aggregatedProperties: Option[Seq[String]] = None
  )(
      implicit readItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[R]],
      filterRequestCodec: JsonValueCodec[FilterRequest[Fi]]
  ): F[ItemsWithCursor[R]] = {
    // avoid sending aggregatedProperties to resources that do not support it
    //implicit val customPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    val body =
      FilterRequest(
        filter,
        limit.map(l => math.min(l, batchSize)).orElse(Some(batchSize)),
        cursor,
        partition.map(_.toString),
        aggregatedProperties
      )
    requestSession.post[ItemsWithCursor[R], ItemsWithCursor[R], FilterRequest[Fi]](
      body,
      uri"$baseUrl/list",
      value => value
    )
  }
  // scalastyle:off parameter.number
}
