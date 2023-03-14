package com.cognite.sdk.scala.v1.fdm.datamodels

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.views.{ViewDefinition, ViewReference}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

/** Type for a data model to reference a view
  */
trait DataModelViewReference

object DataModelViewReference {
  implicit val dataModelViewReferenceEncoder: Encoder[DataModelViewReference] = Encoder.instance {
    case v: ViewDefinition => v.asJson
    case v: ViewReference => v.asJson
    case e =>
      throw new IllegalArgumentException(
        s"'${e.getClass.getSimpleName}' is not a valid DataModelViewReference type"
      )
  }

  implicit val dataModelViewReferenceDecoder: Decoder[DataModelViewReference] =
    List[Decoder[DataModelViewReference]](
      Decoder[ViewDefinition].widen,
      Decoder[ViewReference].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewReference].widen)
}
