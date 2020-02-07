package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.Decoder
import io.circe.derivation.deriveDecoder
import com.softwaremill.sttp._

class ServiceAccounts[F[_]](val requestSession: RequestSession[F])
    extends Readable[ServiceAccount, F] {
  import ServiceAccounts._
  override val baseUrl = uri"${requestSession.baseUrl}/serviceaccounts"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ServiceAccount]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      None,
      None,
      None,
      Constants.defaultBatchSize
    )
}

object ServiceAccounts {
  implicit val serviceAccountDecoder: Decoder[ServiceAccount] =
    deriveDecoder[ServiceAccount]
  implicit val serviceAccountItemsWithCursorDecoder: Decoder[ItemsWithCursor[ServiceAccount]] =
    deriveDecoder[ItemsWithCursor[ServiceAccount]]
}

class ApiKeys[F[_]](val requestSession: RequestSession[F]) extends Readable[ApiKey, F] {
  import ApiKeys._
  override val baseUrl = uri"${requestSession.baseUrl}/apikeys"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[ApiKey]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      None,
      None,
      None,
      Constants.defaultBatchSize
    )
}

object ApiKeys {
  implicit val apiKeyDecoder: Decoder[ApiKey] = deriveDecoder[ApiKey]
  implicit val apiKeyItemsWithCursorDecoder: Decoder[ItemsWithCursor[ApiKey]] =
    deriveDecoder[ItemsWithCursor[ApiKey]]
}

class Groups[F[_]](val requestSession: RequestSession[F]) extends Readable[Group, F] {
  import Groups._
  override val baseUrl = uri"${requestSession.baseUrl}/groups"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Group]] =
    Readable.readWithCursor(
      requestSession,
      baseUrl,
      None,
      None,
      None,
      Constants.defaultBatchSize
    )
}

object Groups {
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val capabilitiesDecoder: Decoder[Capability] = deriveDecoder[Capability]
  implicit val groupDecoder: Decoder[Group] = deriveDecoder[Group]
  implicit val groupItemsWithCursorDecoder: Decoder[ItemsWithCursor[Group]] =
    deriveDecoder[ItemsWithCursor[Group]]
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
