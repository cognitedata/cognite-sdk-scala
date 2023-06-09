package com.cognite.sdk.scala.v1.fdm.datamodels

import cats.implicits.toFunctorOps
import com.cognite.sdk.scala.v1.fdm.views.{ViewCreateDefinition, ViewReference}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

/** Type for a data model to reference a view
  */
trait DataModelCreateViewReference

object DataModelCreateViewReference {
  implicit val dataModelViewReferenceEncoder: Encoder[DataModelCreateViewReference] =
    Encoder.instance {
      case v: ViewCreateDefinition => v.asJson
      case v: ViewReference => v.asJson
      case e =>
        throw new IllegalArgumentException(
          s"'${e.getClass.getSimpleName}' is not a valid DataModelViewCreateReference type"
        )
    }

  implicit val dataModelViewReferenceDecoder: Decoder[DataModelCreateViewReference] =
    List[Decoder[DataModelCreateViewReference]](
      Decoder[ViewCreateDefinition].widen,
      Decoder[ViewReference].widen
    ).reduceLeftOption(_ or _).getOrElse(Decoder[ViewReference].widen)
}
