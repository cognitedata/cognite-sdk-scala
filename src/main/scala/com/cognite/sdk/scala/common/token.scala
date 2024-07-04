// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.implicits.toTraverseOps
import com.cognite.sdk.scala.v1.{Capability, RequestSession}
import sttp.client3._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

import scala.annotation.nowarn

final case class ProjectDetails(projectUrlName: String, groups: Seq[Long])

object ProjectDetails {
  implicit val decoder: Decoder[ProjectDetails] = deriveDecoder[ProjectDetails]
  implicit val encoder: Encoder[ProjectDetails] = deriveEncoder[ProjectDetails]
}

sealed trait ProjectScope

final case class AllProjectsScope() extends ProjectScope
final case class ProjectsListScope(projects: Seq[String]) extends ProjectScope

object ProjectScope {
  private final case class AllProjectsScopeShape(allProjects: Unit)
  implicit val decoder: Decoder[ProjectScope] =
    deriveDecoder[AllProjectsScopeShape]
      .map(_ => AllProjectsScope())
      .or(deriveDecoder[ProjectsListScope].map(x => x))
  implicit val encoderAllProjectScope: Encoder[AllProjectsScope] =
    deriveEncoder[AllProjectsScopeShape]
      .contramap[AllProjectsScope](_ => AllProjectsScopeShape(()))
  implicit val encoderProjectListScope: Encoder[ProjectsListScope] =
    deriveEncoder[ProjectsListScope]
  implicit val encoder: Encoder[ProjectScope] = Encoder.instance {
    case all @ AllProjectsScope() => all.asJson
    case list @ ProjectsListScope(_) => list.asJson
  }
}

final case class ProjectCapability(
    resourceAcl: Map[String, Capability],
    projectScope: ProjectScope
)

@SuppressWarnings(
  Array(
    "org.wartremover.warts.SizeIs"
  )
)
object ProjectCapability {
  implicit val capabilityDecoder: Decoder[Capability] = deriveDecoder
  implicit val capabilityEncoder: Encoder[Capability] = deriveEncoder
  implicit val decoder: Decoder[ProjectCapability] = Decoder.instance { h =>
    val keys = h.keys.map(_.toList).toList.flatten
    val aclKeys = keys.filter(_.endsWith("Acl"))
    for {
      projectScope <- h.downField("projectScope").as[ProjectScope]
      acls <- aclKeys
        .traverse(key => h.downField(key).as[Capability].map((key, _)))
        .map(_.toMap)
      _ <- Either.cond(
        acls.size < 2,
        (),
        DecodingFailure(
          DecodingFailure.Reason.CustomReason(
            "cannot have multiple resourceAcls per capability"
          ),
          h.history
        )
      )
      _ <- Either.cond(
        acls.nonEmpty,
        (),
        DecodingFailure(
          DecodingFailure.Reason.CustomReason("capability must have some resourceAcl"),
          h.history
        )
      )
    } yield new ProjectCapability(acls, projectScope)
  }
  @nowarn("cat=deprecation")
  implicit val encoder: Encoder[ProjectCapability] = Encoder.instance {
    case ProjectCapability(acl, scope) =>
      Json.obj(
        (Seq("projectScope" -> scope.asJson) ++ acl.mapValues(_.asJson).toList): _*
      )
  }
}

final case class TokenInspectResponse(
    subject: String,
    projects: Seq[ProjectDetails],
    capabilities: Seq[ProjectCapability]
)

object TokenInspectResponse {
  implicit val decoder: Decoder[TokenInspectResponse] = deriveDecoder
  implicit val encoder: Encoder[TokenInspectResponse] = deriveEncoder
}

class Token[F[_]](val requestSession: RequestSession[F]) {
  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  implicit val errorOrTokenInspectResponse: Decoder[Either[CdpApiError, TokenInspectResponse]] =
    EitherDecoder.eitherDecoder[CdpApiError, TokenInspectResponse]
  def inspect(): F[TokenInspectResponse] =
    requestSession
      .get[TokenInspectResponse, TokenInspectResponse](
        uri"${requestSession.baseUrl}/api/v1/token/inspect",
        value => value
      )
}
