// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.DataModelProperty._
import fs2.Stream
import io.circe.CursorOp.DownField
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, KeyEncoder, Printer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
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

  def createItems(spaceExternalId: String, model: DataModelIdentifier, overwrite: Boolean = false, items: Seq[DataModelInstance])(implicit F: Async[F]): F[Seq[DataModelInstance]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    dataModels.retrieveByExternalIds(Seq(model.model), model.space.getOrElse("")).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val _: Decoder[DataModelInstance] =
        createDynamicPropertyDecoder(props)

      implicit val dataModelInstanceItemsDecoder: Decoder[Items[DataModelInstance]] =
        deriveDecoder[Items[DataModelInstance]]

      requestSession.post[Seq[DataModelInstance], Items[DataModelInstance], DataModelInstanceCreate](
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

    dataModels.retrieveByExternalIds(Seq(inputQuery.model.model), inputQuery.model.space.getOrElse("")).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val _: Decoder[DataModelInstance] =
        createDynamicPropertyDecoder(props)

      implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
        deriveDecoder[DataModelInstanceQueryResponse]

      requestSession.post[DataModelInstanceQueryResponse, 
        DataModelInstanceQueryResponse, 
        DataModelInstanceQuery](
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
      partition: Option[Partition] = None
  )(implicit F: Async[F]): F[ItemsWithCursor[DataModelInstance]] = {
    val _ = partition // little hack for compilation error parameter value  is never used
    query(inputQuery.copy(cursor = cursor, limit = limit)).flatMap(response =>
      F.pure(ItemsWithCursor(response.items, response.cursor)))
  }

  private[sdk] def queryWithNextCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, DataModelInstance] =
    Readable
      .pullFromCursor(cursor, limit, None, queryWithCursor(inputQuery, _, _, _))
      .stream

  def queryStream(
      inputQuery: DataModelInstanceQuery,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, DataModelInstance] =
    queryWithNextCursor(inputQuery, None, limit)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      model: DataModelIdentifier,
      externalIds: Seq[String]
  )(implicit F: Async[F]): F[DataModelInstanceQueryResponse] =
    dataModels.retrieveByExternalIds(Seq(model.model),model.space.getOrElse("")).flatMap {
      dm =>
        val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

        implicit val _: Decoder[DataModelInstance] =
          createDynamicPropertyDecoder(props)

        implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
          deriveDecoder[DataModelInstanceQueryResponse]

        requestSession.post[DataModelInstanceQueryResponse, 
          DataModelInstanceQueryResponse, DataModelInstanceByExternalId](
          DataModelInstanceByExternalId(externalIds, model),
          uri"$baseUrl/byids",
          value => value
        )
    }
}

object DataModelInstances {

  implicit val dataModelPropertyDeffinitionDecoder: Decoder[DataModelPropertyDeffinition] =
        DataModels.dataModelPropertyDeffinitionDecoder

  implicit val propPrimitiveEncoder: Encoder[DataModelPropertyPrimitive] = {
    case b: BooleanProperty => b.value.asJson
    case i: IntProperty => i.value.asJson
    case bi: BigIntProperty => bi.value.asJson
    case f: Float32Property => f.value.asJson
    case d: Float64Property => d.value.asJson
    case bd: NumericProperty => bd.value.asJson
    case s: TextProperty => s.value.asJson
    case j: JsonProperty => j.value.asJson
    case ts: TimeStampProperty => ts.value.asJson
    case d: DateProperty => d.value.asJson
    case gm: GeometryProperty => gm.value.asJson
    case gg: GeographyProperty => gg.value.asJson
  }

  implicit val propEncoder: Encoder[DataModelProperty] = {
    case p: DataModelPropertyPrimitive => propPrimitiveEncoder(p)
    case dr: DirectRelationProperty => dr.value.asJson
    case v: ArrayProperty[_] =>
      val jsonValues = v.values.map { 
        propPrimitiveEncoder(_)
      }
      Json.fromValues(jsonValues)
  }

  implicit val dataModelInstanceEncoder: Encoder[DataModelInstance] =
    Encoder.encodeMap(KeyEncoder.encodeKeyString, propEncoder).contramap[DataModelInstance](dmi => dmi.properties)

  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    DataModels.dataModelIdentifierEncoder

  implicit val dataModelInstanceCreateEncoder: Encoder[DataModelInstanceCreate] =
    deriveEncoder[DataModelInstanceCreate]

  implicit val dataModelInstanceItemsEncoder: Encoder[Items[DataModelInstanceCreate]] =
    deriveEncoder[Items[DataModelInstanceCreate]]

  implicit val dmiAndFilterEncoder: Encoder[DMIAndFilter] = deriveEncoder[DMIAndFilter]
  implicit val dmiOrFilterEncoder: Encoder[DMIOrFilter] = deriveEncoder[DMIOrFilter]
  implicit val dmiNotFilterEncoder: Encoder[DMINotFilter] = deriveEncoder[DMINotFilter]

  implicit val dmiEqualsFilterEncoder: Encoder[DMIEqualsFilter] = deriveEncoder[DMIEqualsFilter]
  implicit val dmiInFilterEncoder: Encoder[DMIInFilter] = deriveEncoder[DMIInFilter]
  implicit val dmiRangeFilterEncoder: Encoder[DMIRangeFilter] =
    deriveEncoder[DMIRangeFilter].mapJson(_.dropNullValues) // VH TODO make this common

  implicit val dmiPrefixFilterEncoder: Encoder[DMIPrefixFilter] = deriveEncoder[DMIPrefixFilter]
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

  // scalastyle:off cyclomatic.complexity
  private def decodeBaseOnType(c: HCursor, propName: String, propType: PropertyType):
    Either[DecodingFailure, DataModelProperty] =
    propType match {
      case PropertyType.Boolean => c.downField(propName).as[Boolean].map(BooleanProperty(_))
      case PropertyType.Int => c.downField(propName).as[Int].map(IntProperty(_))
      case PropertyType.Bigint => c.downField(propName).as[BigInt].map(BigIntProperty(_))
      case PropertyType.Float32 => c.downField(propName).as[Float].map(Float32Property(_))
      case PropertyType.Float64 => c.downField(propName).as[Double].map(Float64Property(_))
      case PropertyType.Numeric => c.downField(propName).as[BigDecimal].map(NumericProperty(_))
      case PropertyType.Timestamp => c.downField(propName).as[ZonedDateTime].map(TimeStampProperty(_))
      case PropertyType.Date => c.downField(propName).as[LocalDate].map(DateProperty(_))
      case PropertyType.Text => c.downField(propName).as[String].map(TextProperty(_))
      case PropertyType.DirectRelation => c.downField(propName).as[String].map(DirectRelationProperty(_))
      case PropertyType.Geometry => c.downField(propName).as[String].map(GeometryProperty(_))
      case PropertyType.Geography => c.downField(propName).as[String].map(GeographyProperty(_))
      case PropertyType.Array.Boolean => c.downField(propName).as[Seq[Boolean]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(BooleanProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Int => c.downField(propName).as[Seq[Int]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(IntProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Bigint => c.downField(propName).as[Seq[BigInt]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(BigIntProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Float32 => c.downField(propName).as[Seq[Float]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(Float32Property(_))
                                                                .toSeq))
      case PropertyType.Array.Float64 => c.downField(propName).as[Seq[Double]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(Float64Property(_))
                                                                .toSeq))
      case PropertyType.Array.Numeric => c.downField(propName).as[Seq[BigDecimal]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(NumericProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Timestamp => c.downField(propName).as[Seq[ZonedDateTime]] 
                                                          .map(arr => ArrayProperty(
                                                                arr.map(TimeStampProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Date => c.downField(propName).as[Seq[LocalDate]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(DateProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Text => c.downField(propName).as[Seq[String]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(TextProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Geometry => c.downField(propName).as[Seq[String]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(GeometryProperty(_))
                                                                .toSeq))
      case PropertyType.Array.Geography  => c.downField(propName).as[Seq[String]]
                                                          .map(arr => ArrayProperty(
                                                                arr.map(GeographyProperty(_))
                                                                .toSeq))
      case invalidType =>
        throw new Exception(
          s"${invalidType} does not match any property type to decode"
        )
    }

  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf")
  )
  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, DataModelProperty)]],
      props: Map[String, DataModelPropertyDeffinition]
  ): Iterable[Either[DecodingFailure, (String, DataModelProperty)]] =
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
  ): Decoder[DataModelInstance] =
    new Decoder[DataModelInstance] {
      def apply(c: HCursor): Decoder.Result[DataModelInstance] = {
        val res: immutable.Iterable[Either[DecodingFailure, (String, DataModelProperty)]] = props.map {
          case (prop, dmp) =>
            for {
              value <- decodeBaseOnType(c, prop, dmp.`type`)
            } yield value match {
              case p: DataModelProperty => prop -> p
              case _ => // scalastyle:ignore null
                // scala 2 complains match may not be exhaustive with Any while scala 3 complains it's unreachable unless null
                throw new Exception(
                  s"Invalid value when decoding DataModelProperty"
                )
            }
        }
        filterOutNullableProps(res, props).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(new DataModelInstance(res.collect { case Right(value) => value }.toMap))
        }
      }
    }

}
