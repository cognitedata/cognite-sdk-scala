// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.v1.DataModelProperties._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json, Printer}
import sttp.client3._
import sttp.client3.circe._

class DataModels[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {
  import DataModels._
  override val baseUrl = uri"${requestSession.baseUrl}/datamodelstorage/models"

  def createItems(items: Seq[DataModel], spaceExternalId: String): F[Seq[DataModel]] = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
    requestSession.post[Seq[DataModel], Items[DataModel], SpacedItems[DataModel]](
      SpacedItems(spaceExternalId, items),
      uri"$baseUrl",
      value => value.items
    )
  }

  def deleteItems(externalIds: Seq[String], spaceExternalId: String): F[Unit] =
    requestSession.post[Unit, Unit, SpacedItems[CogniteId]](
      SpacedItems(spaceExternalId, externalIds.map(CogniteExternalId(_))),
      uri"$baseUrl/delete",
      _ => ()
    )

  def list(spaceExternalId: String): F[Seq[DataModel]] =
    requestSession.post[Seq[DataModel], Items[DataModel], DataModelListInput](
      DataModelListInput(spaceExternalId),
      uri"$baseUrl/list",
      value => value.items
    )

  def retrieveByExternalIds(
      externalIds: Seq[String],
      spaceExternalId: String
  ): F[Seq[DataModel]] =
    requestSession
      .post[Seq[DataModel], Items[DataModel], SpacedItems[CogniteId]](
        SpacedItems(
          spaceExternalId,
          externalIds.map(CogniteExternalId(_))
        ),
        uri"$baseUrl/byids",
        value => value.items
      )

}

object DataModels {
  implicit val dataModelIdentifierEncoder: Encoder[DataModelIdentifier] =
    Encoder
      .encodeIterable[String, Seq]
      .contramap[DataModelIdentifier] {
        case DataModelIdentifier(Some(space), model) =>
          Seq(space, model)
        case DataModelIdentifier(_, model) =>
          Seq(model)
      }

  implicit val bTreeIndexEncoder: Encoder[BTreeIndex] =
    deriveEncoder[BTreeIndex]
  implicit val dataModelPropertyTypeEncoder: Encoder[AnyPropertyType] =
    Encoder.encodeString.contramap[AnyPropertyType](_.code)
  implicit val dataModelPropertyIndexesEncoder: Encoder[DataModelIndexes] =
    deriveEncoder[DataModelIndexes]
  implicit val dataModelPropertyDeffinitionEncoder: Encoder[DataModelPropertyDeffinition] =
    Encoder.forProduct3[DataModelPropertyDeffinition, AnyPropertyType, Boolean, Option[
      DataModelIdentifier
    ]]("type", "nullable", "targetModel")(pd => (pd.`type`, pd.nullable, pd.targetModel))
  implicit val uniquenessConstraintEncoder: Encoder[UniquenessConstraint] =
    new Encoder[UniquenessConstraint] {
      final def apply(uc: UniquenessConstraint): Json =
        Json.fromFields(
          Seq(
            "uniqueProperties" -> Json.fromValues(
              uc.uniqueProperties.map(p => Json.fromFields(Seq("property" -> Json.fromString(p))))
            )
          )
        )
    }
  implicit val dataModelConstraintsEncoder: Encoder[DataModelConstraints] =
    deriveEncoder[DataModelConstraints]
  implicit val dataModelTypeEncoder: Encoder[DataModelType] =
    deriveEncoder[DataModelType]

  // Derived encoder is used for everything but dataModelType. dataModelType is replaced by
  //   allowEdge and allowNode before returning.
  val derivedDataModelEncoder: Encoder[DataModel] =
    deriveEncoder[DataModel]
  implicit val dataModelEncoder: Encoder[DataModel] =
    new Encoder[DataModel] {
      final def apply(dm: DataModel): Json = {
        val (allowNode, allowEdge) = dm.dataModelType match {
          case DataModelType.Edge => (false, true)
          case DataModelType.Node => (true, false)
        }
        derivedDataModelEncoder(dm)
          .mapObject(
            { "allowEdge" -> Json.fromBoolean(allowEdge) } +: {
              "allowNode" -> Json.fromBoolean(allowNode)
            } +:
              _.filterKeys {
                case "dataModelType" => false
                case _ => true
              }
          )
      }
    }

  implicit val dataModelItemsEncoder: Encoder[SpacedItems[DataModel]] =
    deriveEncoder[SpacedItems[DataModel]]
  implicit val cogniteIdSpacedItemsEncoder: Encoder[SpacedItems[CogniteId]] =
    deriveEncoder[SpacedItems[CogniteId]]
  implicit val dataModelListInputEncoder: Encoder[DataModelListInput] =
    deriveEncoder[DataModelListInput]

  implicit val dataModelIdentifierDecoder: Decoder[DataModelIdentifier] =
    Decoder
      .decodeIterable[String, List]
      .map {
        case modelExternalId :: Nil =>
          DataModelIdentifier(None, modelExternalId)
        case spaceExternalId :: modelExternalId :: Nil =>
          DataModelIdentifier(Some(spaceExternalId), modelExternalId)
        case list =>
          throw new SdkException(
            s"Unable to decode DataModelIdentifier, expected array length 1 or 2, actual length ${list.length.toString}."
          )
      }
  implicit val bTreeIndexDecoder: Decoder[BTreeIndex] =
    deriveDecoder[BTreeIndex]
  implicit val dataModelPropertyTypeDecoder: Decoder[AnyPropertyType] =
    Decoder.decodeString.map(
      PropertyType
        .fromCode(_)
        .getOrElse(
          throw new IllegalArgumentException("Invalid type specified")
        )
    )
  implicit val dataModelPropertyIndexesDecoder: Decoder[DataModelIndexes] =
    deriveDecoder[DataModelIndexes]
  implicit val dataModelPropertyDeffinitionDecoder: Decoder[DataModelPropertyDeffinition] =
    Decoder.forProduct3("type", "nullable", "targetModel")(DataModelPropertyDeffinition.apply)

  implicit val uniquenessConstraintDecoder: Decoder[UniquenessConstraint] =
    new Decoder[UniquenessConstraint] {
      final def apply(c: HCursor): Decoder.Result[UniquenessConstraint] =
        for {
          properties <- c.downField("uniqueProperties").as[Seq[Map[String, String]]]
        } yield new UniquenessConstraint(properties.map(_("property")))
    }
  implicit val dataModelConstraintsDecoder: Decoder[DataModelConstraints] =
    deriveDecoder[DataModelConstraints]
  implicit val dataModelTypeDecoder: Decoder[DataModelType] =
    deriveDecoder[DataModelType]

  // Gets the dataModelType from allowNode and allowEdge, encodes it in a way the derived
  //   decoder will understand, and adds it to the json before parsing
  implicit val dataModelDecoder: Decoder[DataModel] =
    deriveDecoder[DataModel].prepare { init =>
      val allowNode = init.downField("allowNode").as[Boolean]
      val allowEdge = init.downField("allowEdge").as[Boolean]
      val dataModelType: DataModelType = (allowNode, allowEdge) match {
        case (Right(true), Right(false) | Left(_)) => DataModelType.Node
        case (Right(false) | Left(_), Right(true)) => DataModelType.Edge
        case _ => throw new Exception("Exactly one of allowNode and allowEdge must be true")
      }
      init.withFocus(
        _.deepMerge(
          Json.fromFields(
            Seq(
              "dataModelType" -> dataModelTypeEncoder(dataModelType)
            )
          )
        )
      )
    }

  implicit val dataModelItemsDecoder: Decoder[Items[DataModel]] =
    deriveDecoder[Items[DataModel]]
}
