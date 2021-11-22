// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.client3._

class ServiceAccounts[F[_]](val requestSession: RequestSession[F])
    extends Readable[ServiceAccount, F] {
  import ServiceAccounts._
  override val baseUrl = uri"${requestSession.baseUrl}/serviceaccounts"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ServiceAccount]] =
    Readable.readSimple(
      requestSession,
      baseUrl
    )
}

object ServiceAccounts {
  implicit val serviceAccountCodec: JsonValueCodec[ServiceAccount] =
    JsonCodecMaker.make[ServiceAccount]
  implicit val serviceAccountItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[ServiceAccount]] =
    JsonCodecMaker.make[ItemsWithCursor[ServiceAccount]]
}

class ApiKeys[F[_]](val requestSession: RequestSession[F]) extends Readable[ApiKey, F] {
  import ApiKeys._
  override val baseUrl = uri"${requestSession.baseUrl}/apikeys"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ApiKey]] =
    Readable.readSimple(
      requestSession,
      baseUrl
    )
}

object ApiKeys {
  implicit val apiKeyCodec: JsonValueCodec[ApiKey] = JsonCodecMaker.make[ApiKey]
  implicit val apiKeyItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[ApiKey]] =
    JsonCodecMaker.make[ItemsWithCursor[ApiKey]]
}

class Groups[F[_]](val requestSession: RequestSession[F]) extends Readable[Group, F] {
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
}

object Groups {
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val capabilitiesCodec: JsonValueCodec[Capability] = JsonCodecMaker.make[Capability]
  implicit val groupCodec: JsonValueCodec[Group] = JsonCodecMaker.make[Group]
  implicit val groupItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[Group]] =
    JsonCodecMaker.make[ItemsWithCursor[Group]]
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
  implicit val securityCategoryCodec: JsonValueCodec[SecurityCategory] =
    JsonCodecMaker.make[SecurityCategory]
  implicit val groupItemsWithCursorCodec: JsonValueCodec[ItemsWithCursor[SecurityCategory]] =
    JsonCodecMaker.make[ItemsWithCursor[SecurityCategory]]
}
