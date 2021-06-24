// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


final case class TransformConfigRead(
  id: Long,
  name: String,
  query: String,
  destination: Json,
  conflictMode: String,
  isPublic: Boolean = false,
  blacklisted: Option[TransformBlacklistInfo] = None,
  //created: Instant = Instant.ofEpochMilli(0),
  //updated: Instant = Instant.ofEpochMilli(0),
  //owner: TransformConfigOwner,
  ownerIsCurrentUser: Boolean = true,
  hasSourceApiKey: Boolean = false,
  hasDestinationApiKey: Boolean = false,
  hasSourceOidcCredentials: Boolean = false,
  hasDestinationOidcCredentials: Boolean = false,
  //lastFinishedJob: Option[JobDetails],
  //runningJob: Option[JobDetails],
  //schedule: Option[Schedule],
  externalId: Option[String] = None
) extends WithId[Long]
  with WithExternalId
  with WithCreatedTime
  with ToCreate[TransformConfigCreate]
  with ToUpdate[StandardTransformConfigUpdate] {
  override val createdTime: Instant = Instant.ofEpochMilli(0) //TODO

  override def toCreate: TransformConfigCreate =
    TransformConfigCreate(
      name = name,
      query = Some(query),
      destination = Some(destination),
      conflictMode = Some(conflictMode),
      isPublic = Some(isPublic),
      sourceApiKey = None,
      destinationApiKey = None,
      sourceOidcCredentials = None,
      destinationOidcCredentials = None,
      externalId = externalId
    )

  override def toUpdate: StandardTransformConfigUpdate =
    StandardTransformConfigUpdate(
      name = Some(SetValue(name)),
      destination = Some(SetValue(destination)),
      conflictMode = Some(SetValue(conflictMode)),
      query = Some(SetValue(query)),
      //sourceOidcCredentials = if (hasSourceOidcCredentials) { None } else { Some(SetNull()) },
      //destinationOidcCredentials = if (hasDestinationOidcCredentials) { None } else { Some(SetNull()) },
      sourceApiKey = if (hasSourceApiKey) { None } else { Some(SetNull()) },
      destinationApiKey = if (hasDestinationApiKey) { None } else { Some(SetNull()) },
      isPublic = Some(SetValue(isPublic))
    )
}

final case class TransformBlacklistInfo(
  reason: String,
  time: Instant
)
object TransformBlacklistInfo {
  implicit val encoder: Encoder[TransformBlacklistInfo] = deriveEncoder[TransformBlacklistInfo]
  implicit val decoder: Decoder[TransformBlacklistInfo] = deriveDecoder[TransformBlacklistInfo]
}

final case class FlatOidcCredentials(
  clientId: String,
  clientSecret: String,
  scopes: String,
  tokenUri: String,
  cdfProjectName: String
)
object FlatOidcCredentials {
  implicit val encoder: Encoder[FlatOidcCredentials] = deriveEncoder[FlatOidcCredentials]
  implicit val decoder: Decoder[FlatOidcCredentials] = deriveDecoder[FlatOidcCredentials]
}

final case class TransformConfigCreate(
  name: String,
  query: Option[String] = None,
  destination: Option[Json] = None,
  conflictMode: Option[String] = None,
  isPublic: Option[Boolean] = None,
  sourceApiKey: Option[String] = None,
  destinationApiKey: Option[String] = None,
  sourceOidcCredentials: Option[FlatOidcCredentials] = None,
  destinationOidcCredentials: Option[FlatOidcCredentials] = None,
  externalId: Option[String] = None
) extends WithExternalId

final case class StandardTransformConfigUpdate(
  name: Option[NonNullableSetter[String]] = None,
  destination: Option[NonNullableSetter[Json]] = None,
  conflictMode: Option[NonNullableSetter[String]] = None,
  query: Option[NonNullableSetter[String]] = None,
  //sourceOidcCredentials: Option[Setter[FlatOidcCredentialsUpdate]] = None,
  //destinationOidcCredentials: Option[Setter[FlatOidcCredentialsUpdate]] = None,
  sourceApiKey: Option[Setter[String]] = None,
  destinationApiKey: Option[Setter[String]] = None,
  isPublic: Option[NonNullableSetter[Boolean]] = None
)

final case class FlatOidcCredentialsUpdate(
  clientId: Option[String] = None,
  clientSecret: Option[String] = None,
  scopes: Option[String] = None,
  tokenUri: Option[String] = None,
  cdfProjectName: Option[String] = None
)

final case class QueryQuery(query: String)

final case class QueryResponse[T](
    results: Items[T],
    schema: Items[QuerySchemaColumn]
)
final case class QuerySchemaColumn(name: String, sqlType: String, nullable: Boolean)