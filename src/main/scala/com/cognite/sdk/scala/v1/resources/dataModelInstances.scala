// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import cats.syntax.all._
import cats.effect.Async
import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import fs2.Stream
import io.circe.CursorOp.DownField
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, Printer}
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

  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/instances"

  def createItems(items: Items[DataModelInstanceCreate]): F[Seq[DataModelInstanceCreate]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    import DataModelCreate._
    requestSession.post[Seq[DataModelInstanceCreate], Items[DataModelInstanceCreate], Items[
      DataModelInstanceCreate
    ]](
      items,
      uri"$baseUrl/ingest",
      value => value.items
    )
  }

  def query(
      inputQuery: DataModelInstanceQuery
  )(implicit F: Async[F]): F[ItemsWithCursor[DataModelInstanceQueryResponse]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    dataModels.retrieveByExternalIds(Seq(inputQuery.modelExternalId), true).flatMap { dm =>
      val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

      implicit val dynamicPropertyTypeDecoder: Decoder[Map[String, PropertyType]] =
        createDynamicPropertyDecoder(props)

      implicit val dataModelInstanceQueryResponseDecoder: Decoder[DataModelInstanceQueryResponse] =
        new Decoder[DataModelInstanceQueryResponse] {
          def apply(c: HCursor): Decoder.Result[DataModelInstanceQueryResponse] =
            for {
              modelExternalId <- c.downField("modelExternalId").as[String]
              properties <- c.downField("properties").as[Option[Map[String, PropertyType]]]
            } yield DataModelInstanceQueryResponse(modelExternalId, properties)
        }

      implicit val dataModelInstanceQueryResponseItemsWithCursorDecoder
          : Decoder[ItemsWithCursor[DataModelInstanceQueryResponse]] =
        new Decoder[ItemsWithCursor[DataModelInstanceQueryResponse]] {
          def apply(c: HCursor): Decoder.Result[ItemsWithCursor[DataModelInstanceQueryResponse]] =
            for {
              items <- c.downField("items").as[Seq[DataModelInstanceQueryResponse]]
              cursor <- c.downField("cursor").as[Option[String]]
            } yield ItemsWithCursor(items, cursor)
        }

      requestSession.post[ItemsWithCursor[DataModelInstanceQueryResponse], ItemsWithCursor[
        DataModelInstanceQueryResponse
      ], DataModelInstanceQuery](
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
  )(implicit F: Async[F]): F[ItemsWithCursor[DataModelInstanceQueryResponse]] = {
    val _ = partition // little hack for compilation error parameter value  is never used
    query(inputQuery.copy(cursor = cursor, limit = limit))
  }

  private[sdk] def queryWithNextCursor(
      inputQuery: DataModelInstanceQuery,
      cursor: Option[String],
      limit: Option[Int]
  )(implicit F: Async[F]): Stream[F, DataModelInstanceQueryResponse] =
    Readable
      .pullFromCursor(cursor, limit, None, queryWithCursor(inputQuery, _, _, _))
      .stream

  def queryStream(
      inputQuery: DataModelInstanceQuery,
      limit: Option[Int]
  )(implicit F: Async[F]): fs2.Stream[F, DataModelInstanceQueryResponse] =
    queryWithNextCursor(inputQuery, None, limit)

  override def deleteByExternalIds(externalIds: Seq[String]): F[Unit] =
    DeleteByExternalIds.deleteByExternalIds(requestSession, baseUrl, externalIds)

  def retrieveByExternalIds(
      externalIds: Seq[DataModelInstanceByExternalId],
      ignoreUnknownIds: Boolean
  )(implicit F: Async[F]): F[Seq[DataModelInstanceQueryResponse]] =
    dataModels.retrieveByExternalIds(externalIds.map(_.modelExternalId).distinct, true).flatMap {
      dm =>
        val props = dm.headOption.flatMap(_.properties).getOrElse(Map())

        implicit val dynamicPropertyTypeDecoder: Decoder[Map[String, PropertyType]] =
          createDynamicPropertyDecoder(props)

        implicit val dataModelInstanceQueryResponseDecoder
            : Decoder[DataModelInstanceQueryResponse] =
          new Decoder[DataModelInstanceQueryResponse] {
            def apply(c: HCursor): Decoder.Result[DataModelInstanceQueryResponse] =
              for {
                modelExternalId <- c.downField("modelExternalId").as[String]
                properties <- c.downField("properties").as[Option[Map[String, PropertyType]]]
              } yield DataModelInstanceQueryResponse(modelExternalId, properties)
          }

        implicit val dataModelInstanceQueryResponseItemsDecoder
            : Decoder[Items[DataModelInstanceQueryResponse]] =
          new Decoder[Items[DataModelInstanceQueryResponse]] {
            override def apply(c: HCursor): Result[Items[DataModelInstanceQueryResponse]] =
              for {
                items <- c.downField("items").as[Seq[DataModelInstanceQueryResponse]]
              } yield Items(items)
          }

        requestSession.post[Seq[DataModelInstanceQueryResponse], Items[
          DataModelInstanceQueryResponse
        ], ItemsWithIgnoreUnknownIds[DataModelInstanceByExternalId]](
          ItemsWithIgnoreUnknownIds(externalIds, ignoreUnknownIds),
          uri"$baseUrl/byids",
          value => value.items
        )
    }
}

object DataModelInstances {
  implicit val propPrimitiveEncoder: Encoder[PropertyTypePrimitive] = {
    case b: BooleanProperty => b.value.asJson
    case i: Int32Property => i.value.asJson
    case l: Int64Property => l.value.asJson
    case f: Float32Property => f.value.asJson
    case d: Float64Property => d.value.asJson
    case s: StringProperty => s.value.asJson
  }

  implicit val propEncoder: Encoder[PropertyType] = {
    case p: PropertyTypePrimitive => propPrimitiveEncoder(p)
    case dr: DirectRelationProperty => dr.value.asJson
    case ts: TimeStampProperty => ts.value.asJson
    case d: DateProperty => d.value.asJson
    case gm: GeometryProperty => gm.value.asJson
    case gg: GeographyProperty => gg.value.asJson
    case v: ArrayProperty[_] =>
      val jsonValues = v.values.map { case p: PropertyTypePrimitive =>
        propPrimitiveEncoder(p)
      }
      Json.fromValues(jsonValues)
  }

  implicit val dataModelInstanceEncoder: Encoder[DataModelInstanceCreate] =
    new Encoder[DataModelInstanceCreate] {
      final def apply(dmi: DataModelInstanceCreate): Json = Json.obj(
        ("modelExternalId", Json.fromString(dmi.modelExternalId)),
        (
          "properties",
          dmi.properties.asJson
        )
      )
    }

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
  private def decodeBaseOnType(c: HCursor, propName: String, propType: String) =
    propType match {
      case PropertyName.Boolean => c.downField(propName).as[Boolean]
      case PropertyName.Int | PropertyName.Int32 => c.downField(propName).as[Int]
      case PropertyName.Bigint | PropertyName.Int64 => c.downField(propName).as[Long]
      case PropertyName.Float32 => c.downField(propName).as[Float]
      case PropertyName.Float64 | PropertyName.Numeric => c.downField(propName).as[Double]
      case PropertyName.Timestamp => c.downField(propName).as[ZonedDateTime]
      case PropertyName.Date => c.downField(propName).as[LocalDate]
      case PropertyName.Text | PropertyName.DirectRelation | PropertyName.Geometry |
          PropertyName.Geography =>
        c.downField(propName).as[String]
      case PropertyName.ArrayBoolean => c.downField(propName).as[Vector[Boolean]]
      case PropertyName.ArrayInt | PropertyName.ArrayInt32 => c.downField(propName).as[Vector[Int]]
      case PropertyName.ArrayBigint | PropertyName.ArrayInt64 =>
        c.downField(propName).as[Vector[Long]]
      case PropertyName.ArrayFloat32 => c.downField(propName).as[Vector[Float]]
      case PropertyName.ArrayFloat64 | PropertyName.ArrayNumeric =>
        c.downField(propName).as[Vector[Double]]
      case PropertyName.ArrayText => c.downField(propName).as[Vector[String]]
      case invalidType =>
        throw new Exception(
          s"${invalidType} does not match any property type to decode"
        )
    }
  // scalastyle:on cyclomatic.complexity

  private def decodeArrayFromTypeOfFirstElement(
      c: Vector[_],
      propName: String
  ): (String, ArrayProperty[PropertyTypePrimitive]) =
    c.headOption match {
      case Some(_: Boolean) =>
        propName -> ArrayProperty[BooleanProperty](
          c.map(_.asInstanceOf[Boolean]).map(BooleanProperty(_))
        )
      case Some(_: Int) =>
        propName -> ArrayProperty[Int32Property](
          c.map(_.asInstanceOf[Int]).map(Int32Property(_))
        )
      case Some(_: Long) =>
        propName -> ArrayProperty[Int64Property](
          c.map(_.asInstanceOf[Long]).map(Int64Property(_))
        )
      case Some(_: Float) =>
        propName -> ArrayProperty[Float32Property](
          c.map(_.asInstanceOf[Float]).map(Float32Property(_))
        )
      case Some(_: Double) =>
        propName -> ArrayProperty[Float64Property](
          c.map(_.asInstanceOf[Double]).map(Float64Property(_))
        )
      case Some(_: String) =>
        propName -> ArrayProperty[StringProperty](
          c.map(_.asInstanceOf[String]).map(StringProperty(_))
        )
      case _ => propName -> ArrayProperty(Vector())
    }

  private def filterOutNullableProps(
      res: Iterable[Either[DecodingFailure, (String, PropertyType)]],
      props: Map[String, DataModelProperty]
  ): Iterable[Either[DecodingFailure, (String, PropertyType)]] =
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
      props: Map[String, DataModelProperty]
  ): Decoder[Map[String, PropertyType]] =
    new Decoder[Map[String, PropertyType]] {
      def apply(c: HCursor): Decoder.Result[Map[String, PropertyType]] = {
        val res: immutable.Iterable[Either[DecodingFailure, (String, PropertyType)]] = props.map {
          case (prop, dmp) =>
            for {
              value <- decodeBaseOnType(c, prop, dmp.`type`)
            } yield value match {
              case b: Boolean => prop -> BooleanProperty(b)
              case i: Int => prop -> Int32Property(i)
              case l: Long => prop -> Int64Property(l)
              case f: Float => prop -> Float32Property(f)
              case d: Double => prop -> Float64Property(d)
              case ts: ZonedDateTime => prop -> TimeStampProperty(ts)
              case dt: LocalDate => prop -> DateProperty(dt)
              case s: String =>
                dmp.`type` match {
                  case PropertyName.DirectRelation => prop -> DirectRelationProperty(s)
                  case PropertyName.Geometry => prop -> GeographyProperty(s)
                  case PropertyName.Geography => prop -> GeographyProperty(s)
                  case _ => prop -> StringProperty(s)
                }
              case v: Vector[_] =>
                decodeArrayFromTypeOfFirstElement(v, prop)
              case _: Any | null => // scalastyle:ignore null
                // scala 2 complains match may not be exhaustive with Any while scala 3 complains it's unreachable unless null
                throw new Exception(
                  s"Invalid value when decoding DataModelProperty"
                )
            }
        }
        filterOutNullableProps(res, props).find(_.isLeft) match {
          case Some(Left(x)) => Left(x)
          case _ => Right(res.collect { case Right(value) => value }.toMap)
        }
      }
    }
  // scalastyle:on cyclomatic.complexity

}

object DataModelCreate {
  implicit val decodeProp: Decoder[PropertyType] =
    List[Decoder[PropertyType]](
      Decoder.decodeBoolean.map(BooleanProperty(_)).widen,
      Decoder.decodeInt.map(Int32Property(_)).widen,
      Decoder.decodeLong.map(Int64Property(_)).widen,
      Decoder.decodeFloat.map(Float32Property(_)).widen,
      Decoder.decodeDouble.map(Float64Property(_)).widen,
      Decoder.decodeString.map(StringProperty(_)).widen,
      Decoder
        .decodeArray[Boolean]
        .map(x => ArrayProperty[BooleanProperty](x.toVector.map(BooleanProperty(_))))
        .widen,
      Decoder
        .decodeArray[Int]
        .map(x => ArrayProperty[Int32Property](x.toVector.map(Int32Property(_))))
        .widen,
      Decoder
        .decodeArray[Long]
        .map(x => ArrayProperty[Int64Property](x.toVector.map(Int64Property(_))))
        .widen,
      Decoder
        .decodeArray[Float]
        .map(x => ArrayProperty[Float32Property](x.toVector.map(Float32Property(_))))
        .widen,
      Decoder
        .decodeArray[Double]
        .map(x => ArrayProperty[Float64Property](x.toVector.map(Float64Property(_))))
        .widen,
      Decoder
        .decodeArray[String]
        .map(x => ArrayProperty[StringProperty](x.toVector.map(StringProperty(_))))
        .widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder.decodeString.map(StringProperty(_)).widen)
  implicit val dataModelInstanceDecoder: Decoder[DataModelInstanceCreate] =
    new Decoder[DataModelInstanceCreate] {
      def apply(c: HCursor): Decoder.Result[DataModelInstanceCreate] =
        for {
          modelExternalId <- c.downField("modelExternalId").as[String]
          properties <- c.downField("properties").as[Option[Map[String, PropertyType]]]
        } yield DataModelInstanceCreate(modelExternalId, properties)
    }
  implicit val dataModelInstanceItemsDecoder: Decoder[Items[DataModelInstanceCreate]] =
    new Decoder[Items[DataModelInstanceCreate]] {
      def apply(c: HCursor): Decoder.Result[Items[DataModelInstanceCreate]] =
        for {
          items <- c.downField("items").as[Seq[DataModelInstanceCreate]]
        } yield Items(items)
    }
}
