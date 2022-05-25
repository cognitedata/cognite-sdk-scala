// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import PropertyType.AnyProperty
import fs2.Stream
import io.circe.CursorOp.DownField
import io.circe.syntax._
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, KeyEncoder, Printer}
import io.circe.generic.semiauto.deriveEncoder
import sttp.client3._
import sttp.client3.circe._

import java.time.{LocalDate, ZonedDateTime}

import scala.collection.immutable

class DataModelInstances[F[_]](
    val requestSession: RequestSession[F],
    dataModels: DataModels[F]
) extends WithRequestSession[F]
    with DeleteByExternalIds[F]
    with BaseUrl {

  import DataModelInstances._

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/nodes"

  def createItems(
      spaceExternalId: String,
      model: DataModelIdentifier,
      overwrite: Boolean = false,
      items: Seq[PropertyMap]
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
        .post[Seq[PropertyMap], Items[PropertyMap], DataModelInstanceCreate](
          DataModelInstanceCreate(spaceExternalId, model, overwrite, items),
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

        implicit val dataModelInstanceDecoder: Decoder[PropertyMap] =
          createDynamicPropertyDecoder(props)

        implicit val dataModelInstanceSeqDecoder: Decoder[Seq[PropertyMap]] =
          Decoder.decodeIterable[PropertyMap, Seq]

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

      implicit val dataModelInstanceSeqDecoder: Decoder[Seq[PropertyMap]] =
        Decoder.decodeIterable[PropertyMap, Seq]

      implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
        Decoder.forProduct3("items", "modelProperties", "nextCursor")(
          DataModelInstanceQueryResponse.apply
        )

      requestSession.post[
        DataModelInstanceQueryResponse,
        DataModelInstanceQueryResponse,
        DataModelInstanceByExternalId
      ](
        DataModelInstanceByExternalId(externalIds, model),
        uri"$baseUrl/byids",
        value => value
      )
    }
}

object DataModelInstances {

  implicit val dataModelPropertyDeffinitionDecoder: Decoder[DataModelPropertyDeffinition] =
    DataModels.dataModelPropertyDeffinitionDecoder

  // scalastyle:off cyclomatic.complexity
  implicit val propEncoder: Encoder[AnyProperty] = {
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

  implicit val dataModelInstanceEncoder: Encoder[PropertyMap] =
    Encoder
      .encodeMap(KeyEncoder.encodeKeyString, propEncoder)
      .contramap[PropertyMap](dmi => dmi.allProperties)

  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    DataModels.dataModelIdentifierEncoder

  implicit val dataModelInstanceCreateEncoder: Encoder[DataModelInstanceCreate] =
    deriveEncoder[DataModelInstanceCreate]

  implicit val dataModelInstanceItemsEncoder: Encoder[Items[DataModelInstanceCreate]] =
    deriveEncoder[Items[DataModelInstanceCreate]]

  implicit val dmiAndFilterEncoder: Encoder[DMIAndFilter] = deriveEncoder[DMIAndFilter]
  implicit val dmiOrFilterEncoder: Encoder[DMIOrFilter] = deriveEncoder[DMIOrFilter]
  implicit val dmiNotFilterEncoder: Encoder[DMINotFilter] = deriveEncoder[DMINotFilter]

  implicit val dmiEqualsFilterEncoder: Encoder[DMIEqualsFilter] =
    Encoder.forProduct2[DMIEqualsFilter, Seq[String], AnyProperty]("property", "value")(dmiEqF =>
      (dmiEqF.property, dmiEqF.value)
    )
  implicit val dmiInFilterEncoder: Encoder[DMIInFilter] = deriveEncoder[DMIInFilter]
  implicit val dmiRangeFilterEncoder: Encoder[DMIRangeFilter] =
    deriveEncoder[DMIRangeFilter].mapJson(_.dropNullValues) // VH TODO make this common

  implicit val dmiPrefixFilterEncoder: Encoder[DMIPrefixFilter] =
    Encoder.forProduct2[DMIPrefixFilter, Seq[String], AnyProperty]("property", "value")(dmiPxF =>
      (dmiPxF.property, dmiPxF.value)
    )
  implicit val dmiExistsFilterEncoder: Encoder[DMIExistsFilter] = deriveEncoder[DMIExistsFilter]
  implicit val dmiContainsAnyFilterEncoder: Encoder[DMIContainsAnyFilter] =
    deriveEncoder[DMIContainsAnyFilter]
  implicit val dmiContainsAllFilterEncoder: Encoder[DMIContainsAllFilter] =
    deriveEncoder[DMIContainsAllFilter]

  implicit val dmiFilterEncoder: Encoder[DataModelInstanceFilter] = {
    case b: DMIBoolFilter =>
      b match {
        case f: DMIAndFilter => f.asJson
        case f: DMIOrFilter => f.asJson
        case f: DMINotFilter => f.asJson
      }
    case l: DMILeafFilter =>
      l match {
        case f: DMIInFilter => Json.obj(("in", f.asJson))
        case f: DMIEqualsFilter => Json.obj(("equals", f.asJson))
        case f: DMIRangeFilter => Json.obj(("range", f.asJson))
        case f: DMIPrefixFilter => Json.obj(("prefix", f.asJson))
        case f: DMIExistsFilter => Json.obj(("exists", f.asJson))
        case f: DMIContainsAnyFilter => Json.obj(("containsAny", f.asJson))
        case f: DMIContainsAllFilter => Json.obj(("containsAll", f.asJson))
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
        case PropertyType.Bigint => c.as[BigInt]
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
        case PropertyType.Array.Bigint => c.as[Seq[BigInt]]
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
      res: Iterable[Either[DecodingFailure, (String, AnyProperty)]],
      props: Map[String, DataModelPropertyDeffinition]
  ): Iterable[Either[DecodingFailure, (String, AnyProperty)]] =
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
      props: Map[String, DataModelPropertyDeffinition]
  ): Decoder[PropertyMap] =
    new Decoder[PropertyMap] {
      def apply(c: HCursor): Decoder.Result[PropertyMap] = {
        val res: immutable.Iterable[Either[DecodingFailure, (String, AnyProperty)]] =
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
