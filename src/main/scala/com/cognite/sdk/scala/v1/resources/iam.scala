// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.client3._

class Groups[F[_]](val requestSession: RequestSession[F])
    extends Readable[Group, F]
    with Create[Group, GroupCreate, F]
    with DeleteByIds[F, Long] {
  import Groups._
  override val baseUrl = uri"${requestSession.baseUrl}/groups"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Group]] =
    Readable.readSimple(
      requestSession,
      baseUrl
    )

  override def createItems(items: Items[GroupCreate]): F[Seq[Group]] =
    Create.createItems[F, Group, GroupCreate](requestSession, baseUrl, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] = {
    import sttp.client3.circe._
    requestSession.post[Unit, Unit, Seq[Long]](
      ids,
      uri"$baseUrl/delete",
      _ => ()
    )
  }
}

object Groups {
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val capabilitiesDecoder: Decoder[Capability] = deriveDecoder[Capability]
  implicit val groupDecoder: Decoder[Group] = deriveDecoder[Group]
  implicit val groupItemsWithCursorDecoder: Decoder[ItemsWithCursor[Group]] =
    deriveDecoder[ItemsWithCursor[Group]]

  implicit val capabilitiesEncoder: Encoder[Capability] = deriveEncoder
  implicit val groupEncoder: Encoder[GroupCreate] = deriveEncoder
  implicit val groupItemsEncoder: Encoder[Items[GroupCreate]] = deriveEncoder
}

class SecurityCategories[F[_]](val requestSession: RequestSession[F])
    extends Readable[SecurityCategory, F] {
  import SecurityCategories._
  override val baseUrl = uri"${requestSession.baseUrl}/securitycategories"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[SecurityCategory]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      None,
      None,
      None,
      Constants.defaultBatchSize
    )
}

object SecurityCategories {
  implicit val securityCategoryDecoder: Decoder[SecurityCategory] =
    deriveDecoder[SecurityCategory]
  implicit val groupItemsWithCursorDecoder: Decoder[ItemsWithCursor[SecurityCategory]] =
    deriveDecoder[ItemsWithCursor[SecurityCategory]]
}
