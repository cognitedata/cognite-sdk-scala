package com.cognite.sdk.scala.v1.resources
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.softwaremill.sttp._
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

class SequencesResource[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with Readable[Sequence, F]
    with RetrieveByIds[Sequence, F]
    with RetrieveByExternalIds[Sequence, F]
    with Create[Sequence, SequenceCreate, F]
    with DeleteByIds[F, Long]
    with DeleteByExternalIds[F]
    with Search[Sequence, SequenceQuery, F]
    with Update[Sequence, SequenceUpdate, F] {
  import SequencesResource._

  override val baseUri = uri"${requestSession.baseUri}/sequences"

  override private[sdk] def readWithCursor(
      cursor: Option[String],
      limit: Option[Int],
      partition: Option[Partition]
  ): F[ItemsWithCursor[Sequence]] =
    Readable.readWithCursor(
      requestSession,
      baseUri,
      cursor,
      limit,
      None,
      Constants.defaultBatchSize
    )

  override def retrieveByIds(ids: Seq[Long]): F[Seq[Sequence]] =
    RetrieveByIds.retrieveByIds(requestSession, baseUri, ids)

  override def retrieveByExternalIds(externalIds: Seq[String]): F[Seq[Sequence]] =
    RetrieveByExternalIds.retrieveByExternalIds(requestSession, baseUri, externalIds)

  override def createItems(items: Items[SequenceCreate]): F[Seq[Sequence]] =
    Create.createItems[F, Sequence, SequenceCreate](requestSession, baseUri, items)

  override def update(items: Seq[SequenceUpdate]): F[Seq[Sequence]] =
    Update.update[F, Sequence, SequenceUpdate](requestSession, baseUri, items)

  override def deleteByIds(ids: Seq[Long]): F[Unit] =
    DeleteByIds.deleteByIds(requestSession, baseUri, ids)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUri, externalIds)

  override def search(searchQuery: SequenceQuery): F[Seq[Sequence]] =
    Search.search(requestSession, baseUri, searchQuery)
}

object SequencesResource {
  implicit val sequenceColumnEncoder: Encoder[SequenceColumn] = deriveEncoder
  implicit val sequenceColumnCreateEncoder: Encoder[SequenceColumnCreate] = deriveEncoder
  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable"))
  implicit val sequenceColumnDecoder: Decoder[SequenceColumn] = deriveDecoder
  implicit val sequenceDecoder: Decoder[Sequence] = deriveDecoder[Sequence]
  implicit val sequenceUpdateEncoder: Encoder[SequenceUpdate] = deriveEncoder[SequenceUpdate]
  implicit val sequenceItemsWithCursorDecoder: Decoder[ItemsWithCursor[Sequence]] =
    deriveDecoder[ItemsWithCursor[Sequence]]
  implicit val sequenceItemsDecoder: Decoder[Items[Sequence]] =
    deriveDecoder[Items[Sequence]]
  implicit val createSequenceEncoder: Encoder[SequenceCreate] = deriveEncoder[SequenceCreate]
  implicit val createSequenceItemsEncoder: Encoder[Items[SequenceCreate]] =
    deriveEncoder[Items[SequenceCreate]]
  implicit val sequenceFilterEncoder: Encoder[SequenceFilter] =
    deriveEncoder[SequenceFilter]
  implicit val sequenceSearchEncoder: Encoder[SequenceSearch] =
    deriveEncoder[SequenceSearch]
  implicit val sequenceQueryEncoder: Encoder[SequenceQuery] =
    deriveEncoder[SequenceQuery]
}
