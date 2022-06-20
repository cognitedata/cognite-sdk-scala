// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import com.cognite.sdk.scala.common.OAuth2.ClientCredentials

import java.time.Instant
import com.cognite.sdk.scala.common._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.client3.UriContext

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
  implicit val encoder: Encoder[TransformBlacklistInfo] = deriveEncoder[TransformBlacklistInfo]
  implicit val decoder: Decoder[TransformBlacklistInfo] = deriveDecoder[TransformBlacklistInfo]
}

final case class TransformationOwner(user: String)
object TransformationOwner {
  implicit val ownerEncoder: Encoder[TransformationOwner] = deriveEncoder[TransformationOwner]
  implicit val ownerDecoder: Decoder[TransformationOwner] = deriveDecoder[TransformationOwner]
}

sealed trait DestinationDataSource
sealed case class GeneralDataSource(`type`: String) extends DestinationDataSource
sealed case class RawDataSource(`type`: String, database: String, table: String)
    extends DestinationDataSource

sealed case class SequenceRowDataSource(`type`: String, externalId: String)
    extends DestinationDataSource

final case class JobDetails(
    id: Int,
    uuid: String,
    transformationId: Int,
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
    error: String,
    ignoreNullFields: Boolean,
    status: String
)

final case class FlatOidcCredentials(
    clientId: String,
    clientSecret: String,
    scopes: Option[String],
    tokenUri: String,
    cdfProjectName: String,
    audience: Option[String] = None
)

object FlatOidcCredentials {
  implicit val encoder: Encoder[FlatOidcCredentials] = deriveEncoder[FlatOidcCredentials]
  implicit val decoder: Decoder[FlatOidcCredentials] = deriveDecoder[FlatOidcCredentials]

  implicit class ToClientCredentialConverter(flatCredential: FlatOidcCredentials) {
    def toClientCredentials: ClientCredentials =
      ClientCredentials(
        uri"${flatCredential.tokenUri}",
        flatCredential.clientId,
        flatCredential.clientSecret,
        flatCredential.scopes.toList.flatMap(_.split(" ")),
        flatCredential.cdfProjectName,
        flatCredential.audience
      )
  }
  implicit class ToFlatCredentialConverter(credentials: ClientCredentials) {
    def toFlatCredentials: FlatOidcCredentials =
      FlatOidcCredentials(
        credentials.clientId,
        credentials.clientSecret,
        Option(credentials.scopes).filter(_.nonEmpty).map(_.mkString(" ")),
        credentials.tokenUri.toString(),
        credentials.cdfProjectName,
        credentials.audience
      )
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
    sourceOidcCredentials: Option[FlatOidcCredentials] = None,
    destinationOidcCredentials: Option[FlatOidcCredentials] = None,
    externalId: String,
    ignoreNullFields: Boolean,
    dataSetId: Option[Long]
)

final case class TimeFilter(
    min: Option[Long] = None,
    max: Option[Long] = None
)

object TimeFilter {
  implicit val encoder: Encoder[TimeFilter] = deriveEncoder[TimeFilter]
  implicit val decoder: Decoder[TimeFilter] = deriveDecoder[TimeFilter]
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
  implicit val encoder: Encoder[TransformationsFilter] = deriveEncoder[TransformationsFilter]
  implicit val decoder: Decoder[TransformationsFilter] = deriveDecoder[TransformationsFilter]
}
