// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources.fdm.instances

import cats.effect.Async
import cats.syntax.all._
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.RequestSession
import com.cognite.sdk.scala.v1.fdm.instances._
import fs2.Stream
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, JsonObject, Printer}
import sttp.client3._
import sttp.client3.circe._

class Instances[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  import Instances._

  // We need to keep null values. Specifying a null value for a property in InstanceCreate means
  // that we delete the property. Dropping it means that we leave it alone.
  private implicit val nullKeepingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = false)

  override val baseUrl = uri"${requestSession.baseUrl}/models/instances"

  def createItems(instance: InstanceCreate): F[Seq[SlimNodeOrEdge]] =
    requestSession
      .post[Seq[SlimNodeOrEdge], Items[SlimNodeOrEdge], InstanceCreate](
        instance,
        uri"$baseUrl",
        _.items
      )

  def filter(filterRequest: InstanceFilterRequest): F[InstanceFilterResponse] =
    requestSession.post[InstanceFilterResponse, InstanceFilterResponse, InstanceFilterRequest](
      filterRequest,
      uri"$baseUrl/list",
      identity
    )

  def syncRequest(syncRequest: InstanceSyncRequest): F[InstanceSyncResponse] =
    requestSession.post[InstanceSyncResponse, InstanceSyncResponse, InstanceSyncRequest](
      syncRequest,
      uri"$baseUrl/sync",
      identity
    )

  def queryRequest(queryRequest: InstanceQueryRequest): F[InstanceQueryResponse] =
    requestSession.post[InstanceQueryResponse, InstanceQueryResponse, InstanceQueryRequest](
      queryRequest,
      uri"$baseUrl/query",
      identity
    )

  private[sdk] def queryWithCursor(
      inputTableExpression: TableExpression,
      inputSelectExpression: SelectExpression,
      additionalFlags: Map[String, Boolean],
      batchSize: Option[Int],
      cursor: Option[String],
      limit: Option[Int],
      @annotation.nowarn partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[InstanceDefinition]] = {
    val resultName = "query"
    queryRequest(
      InstanceQueryRequest(
        `with` = Map(
          resultName -> inputTableExpression
            .copy(limit =
              Seq(limit.toList, batchSize.toList, inputTableExpression.limit).flatten.minOption
            )
        ),
        cursors = cursor.map(c => Map(resultName -> c)),
        select = Map(resultName -> inputSelectExpression),
        additionalFlags = additionalFlags
      )
    ).map { case InstanceQueryResponse(items, _, cursors) =>
      ItemsWithCursor(
        items.flatMap(_.get(resultName)).getOrElse(Seq.empty),
        cursors.flatMap(_.get(resultName))
      )
    }
  }

  def queryStream(
      inputTableExpression: TableExpression,
      inputSelectExpression: SelectExpression,
      limit: Option[Int],
      additionalFlags: Map[String, Boolean] = Map.empty,
      batchSize: Option[Int] = None
  )(implicit F: Async[F]): Stream[F, InstanceDefinition] =
    Readable
      .pullFromCursor(
        cursor = None,
        maxItemsReturned = limit,
        partition = None,
        get = (cursor, remaining, partition) =>
          queryWithCursor(
            inputTableExpression = inputTableExpression,
            inputSelectExpression = inputSelectExpression,
            additionalFlags = additionalFlags,
            batchSize = batchSize,
            cursor = cursor,
            limit = remaining,
            partition = partition
          )
      )
      .stream

  private[sdk] def filterWithCursor(
      inputQuery: InstanceFilterRequest,
      cursor: Option[String],
      limit: Option[Int],
      @annotation.nowarn partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[InstanceDefinition]] =
    filter(inputQuery.copy(cursor = cursor, limit = limit)).map {
      case InstanceFilterResponse(items, _, cursor, _) =>
        ItemsWithCursor(items, cursor)
    }

  private[sdk] def filterWithNextCursor(
      inputQuery: InstanceFilterRequest,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, InstanceDefinition] =
    Readable
      .pullFromCursor(cursor, limit, None, filterWithCursor(inputQuery, _, _, _))
      .stream

  def filterStream(
      inputQuery: InstanceFilterRequest,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, InstanceDefinition] =
    filterWithNextCursor(inputQuery, None, limit)

  def retrieveByExternalIds(
      items: Seq[InstanceRetrieve],
      sources: Option[Seq[InstanceSource]],
      includeTyping: Boolean = true
  ): F[InstanceFilterResponse] =
    requestSession
      .post[InstanceFilterResponse, InstanceFilterResponse, InstanceRetrieveRequest](
        InstanceRetrieveRequest(items, includeTyping, sources),
        uri"$baseUrl/byids",
        identity
      )

  def delete(instanceRefs: Seq[InstanceDeletionRequest]): F[Seq[InstanceDeletionRequest]] =
    requestSession
      .post[Seq[InstanceDeletionRequest], Items[InstanceDeletionRequest], Items[
        InstanceDeletionRequest
      ]](
        Items(items = instanceRefs),
        uri"$baseUrl/delete",
        _.items
      )
}

object Instances {
  implicit val edgeOrNodeDataEncoder: Encoder[EdgeOrNodeData] = deriveEncoder
  implicit val sourceEncoder: Encoder[InstanceSource] = deriveEncoder
  implicit val instanceRetrieveEncoder: Encoder[InstanceRetrieve] = deriveEncoder
  implicit val instanceDeleteRequestItemsEncoder: Encoder[Items[InstanceDeletionRequest]] =
    deriveEncoder

  implicit val instanceCreateEncoder: Encoder[InstanceCreate] = deriveEncoder
  implicit val viewPropertyReferenceEncoder: Encoder[ViewPropertyReference] = deriveEncoder
  implicit val propertySortV3Encoder: Encoder[PropertySortV3] = deriveEncoder
  implicit val instanceFilterRequestEncoder: Encoder[InstanceFilterRequest] = deriveEncoder
  implicit val instanceDebugEncoder: Encoder[InstanceDebugParameters] = deriveEncoder

  implicit val instanceSyncRequestEncoder: Encoder[InstanceSyncRequest] =
    deriveEncoder[InstanceSyncRequest].mapJsonObject { jsonObj =>
      jsonObj.filter { case (_, v) => !v.isNull }
    }
  implicit val instanceQueryRequestEncoder: Encoder[InstanceQueryRequest] =
    deriveEncoder[InstanceQueryRequest].mapJsonObject { jsonObj =>
      val additionalFields = jsonObj("additionalFlags")
        .flatMap(_.asObject)
        .getOrElse(JsonObject.empty)

      // Order or merge is important, we want jsonObj properties to stay in case of conflicts
      // additionalFlags only contains boolean so deepMerge will go a single level, if this is changed, change this logic too.
      additionalFields
        .deepMerge(jsonObj.remove("additionalFlags"))
        .filter { case (_, v) => !v.isNull }
    }
  implicit val tableExpression: Encoder[TableExpression] =
    deriveEncoder[TableExpression].mapJsonObject { jsonObj =>
      jsonObj.filter { case (_, v) => !v.isNull }
    }
  implicit val nodesTableExpression: Encoder[NodesTableExpression] = deriveEncoder
  implicit val edgeTableExpression: Encoder[EdgeTableExpression] = deriveEncoder
  implicit val selectExpression: Encoder[SelectExpression] = deriveEncoder
  implicit val sourceSelector: Encoder[SourceSelector] = deriveEncoder

  implicit val instanceRetrieveRequestEncoder: Encoder[InstanceRetrieveRequest] = deriveEncoder

  implicit val viewPropertyReferenceDecoder: Decoder[ViewPropertyReference] = deriveDecoder
  implicit val sortDirectionDecoder: Decoder[SortDirection] = deriveDecoder
  implicit val propertySortV3Decoder: Decoder[PropertySortV3] = deriveDecoder
  implicit val InstanceSourceDecoder: Decoder[InstanceSource] = deriveDecoder
  implicit val instanceRetrieveDecoder: Decoder[InstanceRetrieve] = deriveDecoder
  implicit val slimNodeOrEdgeItemsDecoder: Decoder[Items[SlimNodeOrEdge]] = deriveDecoder
  implicit val instanceDeleteRequestItemsDecoder: Decoder[Items[InstanceDeletionRequest]] =
    deriveDecoder
}
