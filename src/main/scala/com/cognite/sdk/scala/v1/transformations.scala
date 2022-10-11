// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.OAuth2.ClientCredentials

import java.time.Instant
import com.cognite.sdk.scala.common._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class TransformationRead(
    id: Long,
    name: String,
    query: String,
    destination: DestinationDataSource,
    conflictMode: String,
    isPublic: Boolean = false,
    blocked: Option[TransformBlacklistInfo],
    createdTime: Instant,
    lastUpdatedTime: Instant,
    owner: TransformationOwner,
    ownerIsCurrentUser: Boolean,
    hasSourceApiKey: Boolean,
    hasDestinationApiKey: Boolean,
    hasSourceOidcCredentials: Boolean,
    hasDestinationOidcCredentials: Boolean,
    lastFinishedJob: Option[JobDetails],
    runningJob: Option[JobDetails],
    externalId: String,
    ignoreNullFields: Boolean,
    dataSetId: Option[Long]
) extends WithId[Long]
    with WithCreatedTime
    with ToCreate[TransformationCreate] {
  override def toCreate: TransformationCreate =
    TransformationCreate(
      name = name,
      query = Some(query),
      destination = Some(destination),
      conflictMode = Some(conflictMode),
      isPublic = Some(isPublic),
      sourceApiKey = None,
      destinationApiKey = None,
      sourceOidcCredentials = None,
      destinationOidcCredentials = None,
      externalId = externalId,
      ignoreNullFields = ignoreNullFields,
      dataSetId = dataSetId
    )
}

final case class TransformBlacklistInfo(
    reason: String,
    createdTime: Instant
)
object TransformBlacklistInfo {
  implicit val transformBlacklistInfoEncoder: Encoder[TransformBlacklistInfo] =
    deriveEncoder[TransformBlacklistInfo]
  implicit val transformBlacklistInfoDecoder: Decoder[TransformBlacklistInfo] =
    deriveDecoder[TransformBlacklistInfo]
}

final case class TransformationOwner(user: String)
object TransformationOwner {
  implicit val transformationOwnerEncoder: Encoder[TransformationOwner] =
    deriveEncoder[TransformationOwner]
  implicit val transformationOwnerDecoder: Decoder[TransformationOwner] =
    deriveDecoder[TransformationOwner]
}

sealed trait DestinationDataSource
sealed case class GeneralDataSource(`type`: String) extends DestinationDataSource
sealed case class RawDataSource(`type`: String, database: String, table: String)
    extends DestinationDataSource

sealed case class SequenceRowDataSource(`type`: String, externalId: String)
    extends DestinationDataSource

final case class TransformationRun(externalId: String)

final case class JobDetails(
    id: Long,
    uuid: String,
    transformationId: Long,
    transformationExternalId: String,
    sourceProject: String,
    destinationProject: String,
    destination: DestinationDataSource,
    conflictMode: String,
    query: String,
    createdTime: Option[Long],
    startedTime: Option[Long],
    finishedTime: Option[Long],
    lastSeenTime: Option[Long],
    error: Option[String],
    ignoreNullFields: Boolean,
    status: String
)

object FlatOidcCredentials {
  implicit val credentialEncoder: Encoder[ClientCredentials] =
    new Encoder[ClientCredentials] {
      final def apply(cc: ClientCredentials): Json = {
        val scopes = Option(cc.scopes).filter(_.nonEmpty).map(_.mkString(" "))
        Json.fromFields(
          Seq(
            "clientId" -> Json.fromString(cc.clientId),
            "clientSecret" -> Json.fromString(cc.clientSecret),
            "tokenUri" -> Json.fromString(cc.tokenUri.toString()),
            "cdfProjectName" -> Json.fromString(cc.cdfProjectName)
          ) ++ cc.audience.map(a => Seq("audience" -> Json.fromString(a))).getOrElse(Seq()) ++
            scopes.map(s => Seq("scopes" -> Json.fromString(s))).getOrElse(Seq())
        )
      }
    }
}

final case class TransformationCreate(
    name: String,
    query: Option[String] = None,
    destination: Option[DestinationDataSource] = None,
    conflictMode: Option[String] = None,
    isPublic: Option[Boolean] = None,
    sourceApiKey: Option[String] = None,
    destinationApiKey: Option[String] = None,
    sourceOidcCredentials: Option[ClientCredentials] = None,
    destinationOidcCredentials: Option[ClientCredentials] = None,
    externalId: String,
    ignoreNullFields: Boolean,
    dataSetId: Option[Long]
)

final case class TimeFilter(
    min: Option[Long] = None,
    max: Option[Long] = None
)

object TimeFilter {
  implicit val timeFilterEncoder: Encoder[TimeFilter] = deriveEncoder[TimeFilter]
  implicit val timeFilterDecoder: Decoder[TimeFilter] = deriveDecoder[TimeFilter]
}

final case class TransformationsFilter(
    isPublic: Option[Boolean] = None,
    nameRegex: Option[String] = None,
    queryRegex: Option[String] = None,
    destinationType: Option[String] = None,
    conflictMode: Option[String] = None,
    hasBlockedError: Option[Boolean] = None,
    cdfProjectName: Option[String] = None,
    createdTime: Option[TimeFilter] = None,
    lastUpdatedTime: Option[TimeFilter] = None,
    dataSetIds: Option[Seq[CogniteId]] = None
)

object TransformationsFilter {
  implicit val transformationsFilterEncoder: Encoder[TransformationsFilter] =
    deriveEncoder[TransformationsFilter]
  implicit val transformationsFilterDecoder: Decoder[TransformationsFilter] =
    deriveDecoder[TransformationsFilter]
}
