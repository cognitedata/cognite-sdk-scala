// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.resources.Nodes.{
  createDynamicPropertyDecoder,
  dataModelInstanceByExternalIdEncoder,
  dataModelInstanceQueryEncoder,
  dataModelNodeCreateEncoder
}
import fs2.Stream
import io.circe.CursorOp.DownField
import io.circe.syntax._
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, KeyEncoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

import java.time.{LocalDate, ZonedDateTime}
import scala.collection.immutable

class Nodes[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with DeleteByExternalIds[F]
    with BaseUrl {

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/nodes"

  def createItems(
      spaceExternalId: String,
      model: DataModelIdentifier,
      overwrite: Boolean = false,
      items: Seq[Node]
  )(implicit F: Async[F]): F[Seq[PropertyMap]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    dataModels.retrieveByExternalIds(Seq(model.model), model.space.getOrElse("")).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val dataModelInstanceDecoder: Decoder[PropertyMap] =
        createDynamicPropertyDecoder(props)

      // for some reason scala complains dataModelInstanceDecoder doesn't seem to be used when
      //   derivedDecoder is used bellow, so an explicit decoder is defined instead
      implicit val dataModelInstanceItemsDecoder: Decoder[Items[PropertyMap]] =
        Decoder.forProduct1("items")(Items.apply[PropertyMap])

      requestSession
        .post[Seq[PropertyMap], Items[PropertyMap], DataModelNodeCreate](
          DataModelNodeCreate(spaceExternalId, model, overwrite, items),
          uri"$baseUrl",
          value => value.items
        )
    }
  }

  def query(
      inputQuery: DataModelInstanceQuery
  )(implicit F: Async[F]): F[DataModelInstanceQueryResponse] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    dataModels
      .retrieveByExternalIds(Seq(inputQuery.model.model), inputQuery.model.space.getOrElse(""))
      .flatMap { dm =>
        val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

        implicit val dataModelNodeDecoder: Decoder[PropertyMap] =
          createDynamicPropertyDecoder(props)

        implicit val dataModelNodeSeqDecoder: Decoder[Seq[PropertyMap]] =
          Decoder.decodeIterable[PropertyMap, Seq]

        import Nodes.dataModelPropertyDefinitionDecoder

        implicit val dataModelInstanceQueryResponseDecoder
            : Decoder[DataModelInstanceQueryResponse] =
          Decoder.forProduct3("items", "modelProperties", "nextCursor")(
            DataModelInstanceQueryResponse.apply
          )

        requestSession.post[
          DataModelInstanceQueryResponse,
          DataModelInstanceQueryResponse,
          DataModelInstanceQuery
        ](
          inputQuery,
          uri"$baseUrl/list",
          value => value
        )
      }
  }

  private[sdk] def queryWithCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int],
      @annotation.nowarn partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[PropertyMap]] =
    query(inputQuery.copy(cursor = cursor, limit = limit)).map {
      case DataModelInstanceQueryResponse(items, _, cursor) =>
        ItemsWithCursor(items, cursor)
    }

  private[sdk] def queryWithNextCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, PropertyMap] =
    Readable
      .pullFromCursor(cursor, limit, None, queryWithCursor(inputQuery, _, _, _))
      .stream

  def queryStream(
      inputQuery: DataModelInstanceQuery,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, PropertyMap] =
    queryWithNextCursor(inputQuery, None, limit)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      externalIds: Seq[String]
  )(implicit F: Async[F]): F[DataModelInstanceQueryResponse] =
    dataModels.retrieveByExternalIds(Seq(model.model), model.space.getOrElse("")).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val dataModelInstanceDecoder: Decoder[PropertyMap] =
        createDynamicPropertyDecoder(props)

      implicit val dataModelNodeSeqDecoder: Decoder[Seq[PropertyMap]] =
        Decoder.decodeIterable[PropertyMap, Seq]

      import Nodes.dataModelPropertyDefinitionDecoder

      implicit val dataModelNodeQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
        Decoder.forProduct3("items", "modelProperties", "nextCursor")(
          DataModelInstanceQueryResponse.apply
        )

      requestSession.post[
        DataModelInstanceQueryResponse,
        DataModelInstanceQueryResponse,
        DataModelInstanceByExternalId
      ](
        DataModelInstanceByExternalId(externalIds.map(CogniteExternalId(_)), model),
        uri"$baseUrl/byids",
        value => value
      )
    }
}

object Nodes {

  implicit val dataModelPropertyDefinitionDecoder: Decoder[DataModelPropertyDefinition] =
    DataModels.dataModelPropertyDefinitionDecoder

  // scalastyle:off cyclomatic.complexity
  implicit val propEncoder: Encoder[DataModelProperty[_]] = {
    case b: PropertyType.Boolean.Property => b.value.asJson
    case i: PropertyType.Int.Property => i.value.asJson
    case bi: PropertyType.Bigint.Property => bi.value.asJson
    case f: PropertyType.Float32.Property => f.value.asJson
    case d: PropertyType.Float64.Property => d.value.asJson
    case bd: PropertyType.Numeric.Property => bd.value.asJson
    case s: PropertyType.Text.Property => s.value.asJson
    case j: PropertyType.Json.Property => j.value.asJson
    case ts: PropertyType.Timestamp.Property => ts.value.asJson
    case d: PropertyType.Date.Property => d.value.asJson
    case gm: PropertyType.Geometry.Property => gm.value.asJson
    case gg: PropertyType.Geography.Property => gg.value.asJson
    case dr: PropertyType.DirectRelation.Property => dr.value.asJson
    case b: PropertyType.Array.Boolean.Property => b.value.asJson
    case i: PropertyType.Array.Int.Property => i.value.asJson
    case bi: PropertyType.Array.Bigint.Property => bi.value.asJson
    case f: PropertyType.Array.Float32.Property => f.value.asJson
    case d: PropertyType.Array.Float64.Property => d.value.asJson
    case bd: PropertyType.Array.Numeric.Property => bd.value.asJson
    case s: PropertyType.Array.Text.Property => s.value.asJson
    case j: PropertyType.Array.Json.Property => j.value.asJson
    case ts: PropertyType.Array.Timestamp.Property => ts.value.asJson
    case d: PropertyType.Array.Date.Property => d.value.asJson
    case gm: PropertyType.Array.Geometry.Property => gm.value.asJson
    case gg: PropertyType.Array.Geography.Property => gg.value.asJson
    case _ => throw new Exception("unknown property type")
  }

  implicit val dataModelPropertyMapEncoder: Encoder[PropertyMap] =
    Encoder
      .encodeMap(KeyEncoder.encodeKeyString, propEncoder)
      .contramap[PropertyMap](dmi => dmi.allProperties)

  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    DataModels.dataModelIdentifierEncoder

  implicit val dataModelNodeCreateEncoder: Encoder[DataModelNodeCreate] =
    deriveEncoder[DataModelNodeCreate]

  implicit val dataModelNodeItemsEncoder: Encoder[Items[DataModelNodeCreate]] =
    deriveEncoder[Items[DataModelNodeCreate]]

  implicit val dmiAndFilterEncoder: Encoder[DSLAndFilter] = deriveEncoder[DSLAndFilter]
  implicit val dmiOrFilterEncoder: Encoder[DSLOrFilter] = deriveEncoder[DSLOrFilter]
  implicit val dmiNotFilterEncoder: Encoder[DSLNotFilter] = deriveEncoder[DSLNotFilter]

  implicit val dmiEqualsFilterEncoder: Encoder[DSLEqualsFilter] =
    Encoder.forProduct2[DSLEqualsFilter, Seq[String], DataModelProperty[_]]("property", "value")(
      dmiEqF => (dmiEqF.property, dmiEqF.value)
    )
  implicit val dmiInFilterEncoder: Encoder[DSLInFilter] = deriveEncoder[DSLInFilter]
  implicit val dmiRangeFilterEncoder: Encoder[DSLRangeFilter] =
    deriveEncoder[DSLRangeFilter].mapJson(_.dropNullValues) // VH TODO make this common

  implicit val dmiPrefixFilterEncoder: Encoder[DSLPrefixFilter] =
    Encoder.forProduct2[DSLPrefixFilter, Seq[String], DataModelProperty[_]]("property", "value")(
      dmiPxF => (dmiPxF.property, dmiPxF.value)
    )
  implicit val dmiExistsFilterEncoder: Encoder[DSLExistsFilter] = deriveEncoder[DSLExistsFilter]
  implicit val dmiContainsAnyFilterEncoder: Encoder[DSLContainsAnyFilter] =
    deriveEncoder[DSLContainsAnyFilter]
  implicit val dmiContainsAllFilterEncoder: Encoder[DSLContainsAllFilter] =
    deriveEncoder[DSLContainsAllFilter]

  implicit val dmiFilterEncoder: Encoder[DomainSpecificLanguageFilter] = {
    case EmptyFilter =>
      Json.fromFields(Seq.empty)
    case b: DSLBoolFilter =>
      b match {
        case f: DSLAndFilter => f.asJson
        case f: DSLOrFilter => f.asJson
        case f: DSLNotFilter => f.asJson
      }
    case l: DSLLeafFilter =>
      l match {
        case f: DSLInFilter => Json.obj(("in", f.asJson))
        case f: DSLEqualsFilter => Json.obj(("equals", f.asJson))
        case f: DSLRangeFilter => Json.obj(("range", f.asJson))
        case f: DSLPrefixFilter => Json.obj(("prefix", f.asJson))
        case f: DSLExistsFilter => Json.obj(("exists", f.asJson))
        case f: DSLContainsAnyFilter => Json.obj(("containsAny", f.asJson))
        case f: DSLContainsAllFilter => Json.obj(("containsAll", f.asJson))
      }
  }

  implicit val dataModelInstanceQueryEncoder: Encoder[DataModelInstanceQuery] =
    deriveEncoder[DataModelInstanceQuery]

  implicit val dataModelInstanceByExternalIdEncoder: Encoder[DataModelInstanceByExternalId] =
    deriveEncoder[DataModelInstanceByExternalId]

  implicit val dmiByExternalIdItemsWithIgnoreUnknownIdsEncoder
      : Encoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]] =
    deriveEncoder[ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]]

  private def decodeBaseOnType[TV](
      c: ACursor,
      t: PropertyType[TV]
  ): Either[DecodingFailure, DataModelProperty[TV]] = {

    // scalastyle:off cyclomatic.complexity
    def decode: Decoder.Result[TV] =
      t match {
        case PropertyType.Boolean => c.as[Boolean]
        case PropertyType.Int => c.as[Int]
        case PropertyType.Int32 => c.as[Int]
        case PropertyType.Int64 => c.as[Long]
        case PropertyType.Bigint => c.as[Long]
        case PropertyType.Float32 => c.as[Float]
        case PropertyType.Float64 => c.as[Double]
        case PropertyType.Numeric => c.as[BigDecimal]
        case PropertyType.Timestamp => c.as[ZonedDateTime]
        case PropertyType.Date => c.as[LocalDate]
        case PropertyType.Text | PropertyType.Json | PropertyType.DirectRelation |
            PropertyType.Geometry | PropertyType.Geography =>
          c.as[String]
        case PropertyType.Array.Boolean => c.as[Seq[Boolean]]
        case PropertyType.Array.Int => c.as[Seq[Int]]
        case PropertyType.Array.Int32 => c.as[Seq[Int]]
        case PropertyType.Array.Int64 => c.as[Seq[Long]]
        case PropertyType.Array.Bigint => c.as[Seq[Long]]
        case PropertyType.Array.Float32 => c.as[Seq[Float]]
        case PropertyType.Array.Float64 => c.as[Seq[Double]]
        case PropertyType.Array.Numeric => c.as[Seq[BigDecimal]]
        case PropertyType.Array.Timestamp => c.as[Seq[ZonedDateTime]]
        case PropertyType.Array.Date => c.as[Seq[LocalDate]]
        case PropertyType.Array.Text | PropertyType.Array.Json | PropertyType.Array.Geometry |
            PropertyType.Array.Geography =>
          c.as[Seq[String]]
      }

    decode.map(v => t.Property(v))
  }

  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf")
  )
  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]],
      props: Map[String, DataModelPropertyDefinition]
  ): Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
    res.filter {
      case Right(_) => true
      case Left(DecodingFailure("Attempt to decode value on failed cursor", downfields)) =>
        val nullableProps = downfields
          .filter(_.isInstanceOf[DownField])
          .map(_.asInstanceOf[DownField])
          .map(_.k)
          .filter(x => x.neqv("properties") && x.neqv("items"))
          .toSet
        !nullableProps.subsetOf(props.filter(_._2.nullable).keySet)
      case _ => true
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  // scalastyle:off cyclomatic.complexity
  def createDynamicPropertyDecoder(
      props: Map[String, DataModelPropertyDefinition]
  ): Decoder[PropertyMap] =
    new Decoder[PropertyMap] {
      def apply(c: HCursor): Decoder.Result[PropertyMap] = {
        val res: immutable.Iterable[Either[DecodingFailure, (String, DataModelProperty[_])]] =
          props.map { case (prop, dmp) =>
            for {
              value <- decodeBaseOnType(c.downField(prop), dmp.`type`)
            } yield prop -> value
          }
        filterOutNullableProps(res, props).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(new PropertyMap(res.collect { case Right(value) => value }.toMap))
        }
      }
    }

}