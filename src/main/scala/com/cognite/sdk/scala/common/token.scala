// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import cats.implicits.toTraverseOps
import com.cognite.sdk.scala.v1.{Capability, RequestSession}
import sttp.client3._
import io.circe.{Decoder, DecodingFailure}
import io.circe.generic.semiauto.deriveDecoder

final case class ProjectDetails(projectUrlName: String, groups: Seq[Long])

object ProjectDetails {
  implicit val decoder: Decoder[ProjectDetails] = deriveDecoder[ProjectDetails]
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
}

final case class TokenInspectResponse(
    subject: String,
    projects: Seq[ProjectDetails],
    capabilities: Seq[ProjectCapability]
)

object TokenInspectResponse {
  implicit val decoder: Decoder[TokenInspectResponse] = deriveDecoder
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
