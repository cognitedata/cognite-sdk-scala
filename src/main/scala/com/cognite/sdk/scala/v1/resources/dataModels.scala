// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.resources

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json, Printer}
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
  implicit val dataModelPropertyTypeEncoder: Encoder[PropertyType] =
    Encoder.encodeString.contramap[PropertyType](_.code)
  implicit val dataModelPropertyIndexesEncoder: Encoder[DataModelIndexes] =
    deriveEncoder[DataModelIndexes]
  implicit val dataModelPropertyEncoder: Encoder[DataModelProperty] =
    deriveEncoder[DataModelProperty]
  implicit val uniquenessConstraintEncoder: Encoder[UniquenessConstraint] =
    deriveEncoder[UniquenessConstraint].mapJson(init =>
      init.hcursor.downField("uniqueProperties").set(
        Json.fromValues(init.hcursor.downField("uniqueProperties").values.get.map(
          v => Json.fromFields(Seq("property" -> v))
          ).toVector
        )
      ).top.get
    )
  implicit val dataModelConstraintsEncoder: Encoder[DataModelConstraints] =
    deriveEncoder[DataModelConstraints]
  implicit val dataModelInstanceTypeEncoder: Encoder[DataModelInstanceType] =
    deriveEncoder[DataModelInstanceType]
  implicit val dataModelEncoder: Encoder[DataModel] =
    deriveEncoder[DataModel].mapJson(init =>
      init.deepMerge(init.hcursor.downField("instanceType").focus.get)
                      .hcursor.downField("instanceType").delete.top.get
      )
  implicit val dataModelItemsEncoder: Encoder[SpacedItems[DataModel]] =
    deriveEncoder[SpacedItems[DataModel]]
  implicit val cogniteIdSpacedItemsEncoder: Encoder[SpacedItems[CogniteId]] =
    deriveEncoder[SpacedItems[CogniteId]]
  implicit val dataModelListInputEncoder: Encoder[DataModelListInput] =
    deriveEncoder[DataModelListInput]

  implicit val dataModelIdentifierDecoder: Decoder[DataModelIdentifier] =
    Decoder.decodeIterable[String, List]
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
  implicit val dataModelPropertyTypeDecoder: Decoder[PropertyType] =
    Decoder.decodeString.map(
      PropertyType
        .fromCode(_)
        .getOrElse(
          throw new IllegalArgumentException("Invalid type specified")
        )
    )
  implicit val dataModelPropertyIndexesDecoder: Decoder[DataModelIndexes] =
    deriveDecoder[DataModelIndexes]
  implicit val dataModelPropertyDecoder: Decoder[DataModelProperty] =
    deriveDecoder[DataModelProperty]
  implicit val uniquenessConstraintDecoder: Decoder[UniquenessConstraint] =
    deriveDecoder[UniquenessConstraint].prepare( init =>
      init.downField("uniqueProperties").set(
        Json.fromValues(init.downField("uniqueProperties").values.get.map(
          v => v.hcursor.downField("property").focus.get
          ).toVector
        )
      )
    )
  implicit val dataModelConstraintsDecoder: Decoder[DataModelConstraints] =
    deriveDecoder[DataModelConstraints]
  implicit val dataModelInstanceTypeDecoder: Decoder[DataModelInstanceType] =
    deriveDecoder[DataModelInstanceType]
  implicit val dataModelDecoder: Decoder[DataModel] =
    deriveDecoder[DataModel].prepare(init => {
      val instanceType: Json = dataModelInstanceTypeEncoder(
        init.as[DataModelInstanceType].right.get)      

      init.withFocus( json => 
        Json.fromJsonObject(
          json.asObject.get.+:
            ("instanceType" -> instanceType)
        ))
    })
  implicit val dataModelItemsDecoder: Decoder[Items[DataModel]] =
    deriveDecoder[Items[DataModel]]
}
