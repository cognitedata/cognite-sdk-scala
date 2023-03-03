// Copyright 2023 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.playground.resources

import cats.Monad
import cats.syntax.all._
import com.cognite.sdk.scala.common.{CdpApiError, Items, ItemsWithCursor}
import com.cognite.sdk.scala.playground._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder, Json, JsonObject}
import io.circe.syntax._
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

class WellDataLayer[F[_]: Monad](val requestSession: RequestSession[F]) {
  import CdpApiError._
  import WellDataLayer._

  lazy val wells = new WellDataLayerWells(requestSession)
  lazy val wellSources = new WellDataLayerWellSources(requestSession)
  lazy val wellbores = new WellDataLayerWellbores(requestSession)
  lazy val wellboreSources = new WellDataLayerWellboreSources(requestSession)
  lazy val sources = new WellDataLayerSources(requestSession, wellSources)

  private def get[R, T](
      uri: Uri,
      mapResult: T => R
  )(implicit decoder: Decoder[T]): F[R] =
    requestSession.get(uri, mapResult)

  private def post[R, T, I](
      body: I,
      uri: Uri,
      mapResult: T => R
  )(implicit serializer: BodySerializer[I], decoder: Decoder[T]): F[R] =
    requestSession.post(body, uri, mapResult)

  def getSchema(modelName: String): F[String] = {
    val uri = uri"${requestSession.baseUrl}/wdl/spark/structtypes/$modelName"
    val response: F[Either[String, String]] = requestSession
      .sendCdf { request =>
        request
          .get(uri)
          .response(asString)
      }

    response.map {
      case Left(rawError) =>
        decode[CdpApiError](rawError) match {
          case Left(deserializationError) => throw deserializationError
          case Right(cdpApiError) => throw cdpApiError.asException(uri, None)
        }
      case Right(schema) => schema
    }
  }

  def setItems(urlPart: String, items: Seq[JsonObject]): F[Unit] =
    if (items.isEmpty) {
      Monad[F].unit
    } else {
      // uri strings will escape slashes when string interpolation, but a list of strings works fine.
      val body = Items(items)
      val urlParts = urlPart.split("/")
      post[Unit, EmptyObj, Items[JsonObject]](
        body,
        uri"${requestSession.baseUrl}/wdl/$urlParts",
        _ => ()
      )
    }

  def listItemsWithPost(
      urlPart: String,
      cursor: Option[String] = None,
      limit: Option[Int] = None,
      transformBody: (JsonObject) => JsonObject = it => it
  ): F[ItemsWithCursor[JsonObject]] = {
    val jsonObject = LimitAndCursor(cursor, limit).asJson.asObject
      .getOrElse(throw new Exception("unreachable"))
    val json = Json.fromJsonObject(transformBody(jsonObject))
    val urlParts = urlPart.split("/")
    post[ItemsWithCursor[JsonObject], ItemsWithCursor[JsonObject], Json](
      json,
      uri"${requestSession.baseUrl}/wdl/$urlParts",
      value => value
    )
  }

  def listItemsWithGet(urlPart: String): F[ItemsWithCursor[JsonObject]] = {
    val urlParts = urlPart.split("/")
    get[ItemsWithCursor[JsonObject], ItemsWithCursor[JsonObject]](
      uri"${requestSession.baseUrl}/wdl/$urlParts",
      value => value
    )
  }
}

class WellDataLayerSources[F[_]: Monad](
    val requestSession: RequestSession[F],
    private val wellSources: WellDataLayerWellSources[F]
) {
  import WellDataLayer._

  def list(): F[Seq[Source]] = // follow cursors here.
    requestSession.get[Seq[Source], SourceItems](
      uri"${requestSession.baseUrl}/wdl/sources",
      value => value.items
    )

  def create(items: Seq[Source]): F[Seq[Source]] =
    if (items.isEmpty) {
      Monad[F].pure(Seq())
    } else {
      val body = SourceItems(items)
      requestSession.post[Seq[Source], SourceItems, SourceItems](
        body,
        uri"${requestSession.baseUrl}/wdl/sources",
        value => value.items
      )
    }

  def delete(items: Seq[Source]): F[Unit] =
    if (items.isEmpty) {
      Monad[F].unit
    } else {
      val body = DeleteSources(items)
      requestSession.post[Unit, EmptyObj, DeleteSources](
        body,
        uri"${requestSession.baseUrl}/wdl/sources/delete",
        _ => ()
      )
    }

  def deleteRecursive(items: Seq[Source]): F[Unit] = {
    val namesToDelete = items.map(_.name)
    for {
      wells <- wellSources.list(WellSourceFilter(Some(namesToDelete)))
      wellSources = wells
        .map(_.source)
        .filter(assetSource => namesToDelete.contains(assetSource.sourceName))
      _ <- this.wellSources.deleteRecursive(wellSources)
      _ <- delete(items)
    } yield ()
  }
}

class WellDataLayerWellSources[F[_]: Monad](val requestSession: RequestSession[F]) {
  import WellDataLayer._

  def list(
      filter: WellSourceFilter = WellSourceFilter(),
      cursor: Option[String] = None,
      limit: Option[Int] = None
  ): F[Seq[WellSource]] = {
    val body = WellSourceFilterRequest(filter, cursor, limit)
    requestSession.post[Seq[WellSource], WellSourceItems, WellSourceFilterRequest](
      body,
      uri"${requestSession.baseUrl}/wdl/wellsources/list",
      value => value.items
    )
  }

  def delete(items: Seq[AssetSource]): F[Unit] = delete(items, recursive = false)

  def deleteRecursive(items: Seq[AssetSource]): F[Unit] = delete(items, recursive = true)

  private def delete(items: Seq[AssetSource], recursive: Boolean): F[Unit] =
    if (items.isEmpty) {
      Monad[F].unit
    } else {
      requestSession.post[Unit, EmptyObj, DeleteWells](
        DeleteWells(items, recursive),
        uri"${requestSession.baseUrl}/wdl/wells/delete",
        _ => ()
      )
    }
}

class WellDataLayerWells[F[_]: Monad](val requestSession: RequestSession[F]) {
  import WellDataLayer._

  def list(
      filter: WellFilter = WellFilter(),
      cursor: Option[String] = None,
      limit: Option[Int] = None
  ): F[Seq[Well]] = {
    val body = WellFilterRequest(filter, cursor, limit)
    requestSession.post[Seq[Well], WellItems, WellFilterRequest](
      body,
      uri"${requestSession.baseUrl}/wdl/wells/list",
      value => value.items
    )
  }

  def create(items: Seq[WellSource]): F[Seq[Well]] =
    if (items.isEmpty) {
      Monad[F].pure(Seq())
    } else {
      val body = WellSourceItems(items)
      requestSession.post[Seq[Well], WellItems, WellSourceItems](
        body,
        uri"${requestSession.baseUrl}/wdl/wells",
        value => value.items
      )
    }

  def setMergeRules(rules: WellMergeRules): F[Unit] =
    requestSession.post[Unit, EmptyObj, WellMergeRules](
      rules,
      uri"${requestSession.baseUrl}/wdl/wells/mergerules",
      _ => ()
    )
}

class WellDataLayerWellboreSources[F[_]](val requestSession: RequestSession[F]) {
  import WellDataLayer._

  def list(
      filter: WellboreSourceFilter = WellboreSourceFilter(),
      cursor: Option[String] = None,
      limit: Option[Int] = None
  ): F[Seq[WellboreSource]] = {
    val body = WellboreSourceFilterRequest(filter, cursor, limit)
    requestSession.post[Seq[WellboreSource], WellboreSourceItems, WellboreSourceFilterRequest](
      body,
      uri"${requestSession.baseUrl}/wdl/wellboresources/list",
      value => value.items
    )
  }
}

class WellDataLayerWellbores[F[_]: Monad](val requestSession: RequestSession[F]) {
  import WellDataLayer._

  def create(items: Seq[WellboreSource]): F[Seq[Wellbore]] =
    if (items.isEmpty) {
      Monad[F].pure(Seq())
    } else {
      val body = WellboreSourceItems(items)
      requestSession.post[Seq[Wellbore], WellboreItems, WellboreSourceItems](
        body,
        uri"${requestSession.baseUrl}/wdl/wellbores",
        value => value.items
      )
    }

  def setMergeRules(rules: WellboreMergeRules): F[Unit] =
    requestSession.post[Unit, EmptyObj, WellboreMergeRules](
      rules,
      uri"${requestSession.baseUrl}/wdl/wellbores/mergerules",
      _ => ()
    )
}

object WellDataLayer {
  implicit val sourceEncoder: Encoder[Source] = deriveEncoder
  implicit val sourceDecoder: Decoder[Source] = deriveDecoder

  implicit val deleteSourcesEncoder: Encoder[DeleteSources] = deriveEncoder
  implicit val sourceItemsDecoder: Decoder[SourceItems] = deriveDecoder
  implicit val sourceItemsEncoder: Encoder[SourceItems] = deriveEncoder

  implicit val assetSourceEncoder: Encoder[AssetSource] = deriveEncoder
  implicit val assetSourceDecoder: Decoder[AssetSource] = deriveDecoder
  implicit val deleteWellsEncoder: Encoder[DeleteWells] = deriveEncoder

  implicit val distanceEncoder: Encoder[Distance] = deriveEncoder
  implicit val distanceDecoder: Decoder[Distance] = deriveDecoder
  implicit val wellheadDecoder: Decoder[Wellhead] = deriveDecoder
  implicit val wellheadEncoder: Encoder[Wellhead] = deriveEncoder
  implicit val datumDecoder: Decoder[Datum] = deriveDecoder
  implicit val datumEncoder: Encoder[Datum] = deriveEncoder

  implicit val wellboreDecoder: Decoder[Wellbore] = deriveDecoder

  implicit val wellDecoder: Decoder[Well] = deriveDecoder

  implicit val wellSourceEncoder: Encoder[WellSource] = deriveEncoder
  implicit val wellSourceItemsEncoder: Encoder[WellSourceItems] = deriveEncoder
  implicit val wellItemsDecoder: Decoder[WellItems] = deriveDecoder
  implicit val wellMergeRulesEncoder: Encoder[WellMergeRules] = deriveEncoder
  implicit val wellSourceDecoder: Decoder[WellSource] = deriveDecoder
  implicit val wellSourceItemsDecoder: Decoder[WellSourceItems] = deriveDecoder

  implicit val wellboreSourceEncoder: Encoder[WellboreSource] = deriveEncoder
  implicit val wellboreSourceItemsEncoder: Encoder[WellboreSourceItems] = deriveEncoder
  implicit val wellboreItemsDecoder: Decoder[WellboreItems] = deriveDecoder
  implicit val wellboreMergeRulesEncoder: Encoder[WellboreMergeRules] = deriveEncoder
  implicit val wellboreSourceDecoder: Decoder[WellboreSource] = deriveDecoder
  implicit val wellboreSourceItemsDecoder: Decoder[WellboreSourceItems] = deriveDecoder

  implicit val emptyObjDecoder: Decoder[EmptyObj] = deriveDecoder
  implicit val emptyObjEncoder: Encoder[EmptyObj] = deriveEncoder

  implicit val jsonObjectItemsEncoder: Encoder[Items[JsonObject]] = deriveEncoder
  implicit val jsonObjectItemsWithCursorDecoder: Decoder[ItemsWithCursor[JsonObject]] =
    deriveDecoder

  implicit val wellFilterEncoder: Encoder[WellFilter] = deriveEncoder
  implicit val wellFilterRequestEncoder: Encoder[WellFilterRequest] = deriveEncoder
  implicit val wellSourceFilterEncoder: Encoder[WellSourceFilter] = deriveEncoder
  implicit val wellSourceFilterRequestEncoder: Encoder[WellSourceFilterRequest] = deriveEncoder
  implicit val wellboreSourceFilterEncoder: Encoder[WellboreSourceFilter] = deriveEncoder
  implicit val wellboreSourceFilterRequestEncoder: Encoder[WellboreSourceFilterRequest] =
    deriveEncoder

  implicit val limitAndCursorEncoder: Encoder[LimitAndCursor] = deriveEncoder
}
