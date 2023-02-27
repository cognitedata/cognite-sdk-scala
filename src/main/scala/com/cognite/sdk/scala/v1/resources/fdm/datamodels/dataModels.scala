package com.cognite.sdk.scala.v1.resources.fdm.datamodels

import com.cognite.sdk.scala.common._
import com.cognite.sdk.scala.v1.RequestSession
import com.cognite.sdk.scala.v1.fdm.common.DataModelReference
import com.cognite.sdk.scala.v1.fdm.datamodels.{DataModel, DataModelCreate}
import io.circe.Printer
import sttp.client3.UriContext
import sttp.client3.circe._

class DataModels[F[_]](val requestSession: RequestSession[F])
    extends WithRequestSession[F]
    with BaseUrl {

  implicit val nullDroppingPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)

  override val baseUrl = uri"${requestSession.baseUrl}/models/datamodels"

  def createItems(items: Seq[DataModelCreate]): F[Seq[DataModel]] =
    requestSession
      .post[Seq[DataModel], Items[DataModel], Items[DataModelCreate]](
        Items(items),
        uri"$baseUrl",
        value => value.items
      )

  def retrieveItems(
      items: Seq[DataModelReference],
      inlineViews: Option[Boolean] = None
  ): F[Seq[DataModel]] =
    requestSession.post[Seq[DataModel], Items[DataModel], Items[DataModelReference]](
      Items(items),
      uri"$baseUrl/byids"
        .addParam("inlineViews", inlineViews.map(_.toString)),
      value => value.items
    )
}
