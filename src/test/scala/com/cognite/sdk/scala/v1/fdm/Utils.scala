package com.cognite.sdk.scala.v1.fdm

import com.cognite.sdk.scala.v1.fdm.common.Usage
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.{DirectNodeRelationProperty, PrimitiveProperty, TextProperty}
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.containers.{ContainerConstraint, ContainerCreateDefinition, IndexDefinition}
import com.cognite.sdk.scala.v1.fdm.instances.InstancePropertyValue
import io.circe.{Json, JsonObject}

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import scala.util.Random

object Utils {

  // scalastyle:off
  def createAllPossibleContainerPropCombinations: Map[String, ContainerPropertyDefinition] = {
    val propertyTypes = List[PropertyType](
      TextProperty(),
      PrimitiveProperty(`type` = PrimitivePropType.Boolean),
      PrimitiveProperty(`type` = PrimitivePropType.Float32),
      PrimitiveProperty(`type` = PrimitivePropType.Float64),
      PrimitiveProperty(`type` = PrimitivePropType.Int32),
      PrimitiveProperty(`type` = PrimitivePropType.Int64),
      PrimitiveProperty(`type` = PrimitivePropType.Numeric),
      PrimitiveProperty(`type` = PrimitivePropType.Timestamp),
      PrimitiveProperty(`type` = PrimitivePropType.Date),
      PrimitiveProperty(`type` = PrimitivePropType.Json),
      TextProperty(list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Boolean, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Float32, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Float64, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Int32, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Int64, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Numeric, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Timestamp, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Date, list = Some(true)),
      PrimitiveProperty(`type` = PrimitivePropType.Json, list = Some(true)),
      DirectNodeRelationProperty(container = None)
    )
    val boolOptions = List(
      true,
      false
    )

    val allPossibleProperties = (for {
      p <- propertyTypes
      nullable <- boolOptions
      withDefault <- boolOptions
      withAutoIncrement <- boolOptions
    } yield {
      val defaultValue: Option[PropertyDefaultValue] = if (withDefault && !p.isList) {
        p match {
          case TextProperty(_, _) => Some(PropertyDefaultValue.String("defaultTextValue"))
          case PrimitiveProperty(PrimitivePropType.Boolean, _) => Some(PropertyDefaultValue.Boolean(false))
          case PrimitiveProperty(PrimitivePropType.Float32, _) => Some(PropertyDefaultValue.Float32(1.2F))
          case PrimitiveProperty(PrimitivePropType.Float64, _) => Some(PropertyDefaultValue.Float64(1.21))
          case PrimitiveProperty(PrimitivePropType.Int32, _) => Some(PropertyDefaultValue.Int32(1))
          case PrimitiveProperty(PrimitivePropType.Int64, _) => Some(PropertyDefaultValue.Int64(12L))
          case PrimitiveProperty(PrimitivePropType.Numeric, _) => Some(PropertyDefaultValue.Numeric(BigDecimal(123)))
          case PrimitiveProperty(PrimitivePropType.Timestamp, _) =>
            Some(
              PropertyDefaultValue.String(
                LocalDateTime.now().atZone(ZoneId.of("UTC")).format(InstancePropertyValue.Timestamp.formatter)
              )
            )
          case PrimitiveProperty(PrimitivePropType.Date, _) =>
            Some(PropertyDefaultValue.String(LocalDate.now().format(InstancePropertyValue.Date.formatter)))
          case PrimitiveProperty(PrimitivePropType.Json, _) =>
            Some(
              PropertyDefaultValue.Object(
                Json.fromJsonObject(
                  JsonObject.fromMap(
                    Map(
                      "a" -> Json.fromString("a"),
                      "b" -> Json.fromInt(1)
                    )
                  )
                )
              )
            )
          case DirectNodeRelationProperty(_) => None
        }
      } else {
        None
      }

      val autoIncrement = if (withAutoIncrement) {
        defaultValue match {
          case Some(_: PropertyDefaultValue.Int32) => true
          case Some(_: PropertyDefaultValue.Int64) => true
          case _ => false
        }
      } else {
        false
      }

      val nullability = if (p.isInstanceOf[DirectNodeRelationProperty]) true
      else nullable

      val nameComponents = List(
        if (p.isInstanceOf[PrimitiveProperty]) p.asInstanceOf[PrimitiveProperty].`type`.productPrefix else p.getClass.getSimpleName,
        if (p.isList) "List" else "NonList",
        if (autoIncrement) "AutoIncrementing" else "NotAutoIncrementing",
        if (defaultValue.nonEmpty) "WithDefaultValue" else "WithoutDefaultValue",
        if (nullability) "Nullable" else "NonNullable",
      ).filter(_.nonEmpty)

      s"${nameComponents.mkString("")}" -> ContainerPropertyDefinition(
        nullable = Some(nullability),
        autoIncrement = Some(autoIncrement),
        defaultValue = defaultValue,
        description = Some(s"Test ${nameComponents.mkString(" ")} Description"),
        name = Some(s"Test-${nameComponents.mkString("-")}-Name"),
        `type` = p
      )
    }).distinct.toMap
    allPossibleProperties
  }

  def createTestContainer(space: String, containerExternalId: String, usage: Usage): ContainerCreateDefinition = {
    val allPossibleProperties: Map[String, ContainerPropertyDefinition] = createAllPossibleContainerPropCombinations
    val allPossiblePropertyKeys = allPossibleProperties.keys.toList

    val constraints: Map[String, ContainerConstraint] = Map(
      "uniqueConstraint" -> ContainerConstraint.UniquenessConstraint(
        allPossiblePropertyKeys.take(5)
      )
    )

    val indexes: Map[String, IndexDefinition] = Map(
      "index1" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.take(2)),
      "index2" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.slice(5, 7))
    )

    val containerToCreate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"Test-${usage.productPrefix}-Container-$containerExternalId-Name"),
      description = Some(s"Test ${usage.productPrefix} Container $containerExternalId Description"),
      usedFor = Some(usage),
      properties = allPossibleProperties,
      constraints = Some(constraints),
      indexes = Some(indexes)
    )

    containerToCreate
  }

  def createInstancePropertyForContainerProperty(propName: String, containerPropType: PropertyType): InstancePropertyValue = {
    if(containerPropType.isList) {
      listContainerPropToInstanceProperty(propName, containerPropType)
    } else {
      nonListContainerPropToInstanceProperty(propName, containerPropType)
    }
  }


  private def listContainerPropToInstanceProperty(propName: String, containerPropType: PropertyType): InstancePropertyValue = {
      containerPropType match {
        case PropertyType.TextProperty(Some(true), _) =>
          InstancePropertyValue.StringList(List(s"${propName}Value1", s"${propName}Value2"))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(true)) =>
          InstancePropertyValue.BooleanList(List(true, false, true, false))
        case PropertyType.PrimitiveProperty(
        PrimitivePropType.Int32 | PrimitivePropType.Int64,
        Some(true)
        ) =>
          InstancePropertyValue.IntegerList((1 to 10).map(_ => Random.nextLong()).toList)
        case PropertyType.PrimitiveProperty(
        PrimitivePropType.Float32 | PrimitivePropType.Float64,
        Some(true)
        ) =>
          InstancePropertyValue.DoubleList((1 to 10).map(_ => Random.nextDouble()).toList)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Numeric, Some(true)) =>
          InstancePropertyValue.DoubleList((1 to 10).map(_ => Random.nextDouble()).toList)
        case PropertyType.PrimitiveProperty(PrimitivePropType.Date, Some(true)) =>
          InstancePropertyValue.DateList((1 to 10).toList.map(i => LocalDate.now().minusDays(i.toLong)))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(true)) =>
          InstancePropertyValue.TimestampList((1 to 10).toList.map(i => ZonedDateTime.now().minusDays(i.toLong)))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Json, Some(true)) =>
          InstancePropertyValue.ObjectList(
            List(
              Json.fromJsonObject(
                JsonObject.fromMap(
                  Map(
                    "a" -> Json.fromString("a"),
                    "b" -> Json.fromInt(1),
                    "c" -> Json.fromBoolean(true),
                  )
                )
              ),
              Json.fromJsonObject(
                JsonObject.fromMap(
                  Map(
                    "a" -> Json.fromString("b"),
                    "b" -> Json.fromInt(1),
                    "c" -> Json.fromBoolean(false),
                    "d" -> Json.fromDoubleOrString(1.56),
                  )
                )
              )
            )
          )
        case other => throw new IllegalArgumentException(s"Unknown value :${other.toString}")
      }
  }


  private def nonListContainerPropToInstanceProperty(propName: String, containerPropType: PropertyType): InstancePropertyValue = {
      containerPropType match {
        case PropertyType.TextProperty(None | Some(false), _) =>
          InstancePropertyValue.String(s"${propName}Value")
        case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
          InstancePropertyValue.Boolean(false)
        case PropertyType.PrimitiveProperty(
        PrimitivePropType.Int32 | PrimitivePropType.Int64,
        None | Some(false)
        ) =>
          InstancePropertyValue.Integer(Random.nextLong())
        case PropertyType.PrimitiveProperty(
        PrimitivePropType.Float32 | PrimitivePropType.Float64,
        None | Some(false)
        ) =>
          InstancePropertyValue.Double(Random.nextDouble())
        case PropertyType.PrimitiveProperty(PrimitivePropType.Numeric, None | Some(false)) =>
          InstancePropertyValue.Double(Random.nextDouble())
        case PropertyType.PrimitiveProperty(PrimitivePropType.Date, None | Some(false)) =>
          InstancePropertyValue.Date(LocalDate.now().minusDays(Random.nextInt(30).toLong))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, None | Some(false)) =>
          InstancePropertyValue.Timestamp(ZonedDateTime.now().minusDays(Random.nextInt(30).toLong))
        case PropertyType.PrimitiveProperty(PrimitivePropType.Json, None | Some(false)) =>
          InstancePropertyValue.Object(Json.fromJsonObject(
            JsonObject.fromMap(
              Map(
                "a" -> Json.fromString("a"),
                "b" -> Json.fromInt(1),
                "c" -> Json.fromBoolean(true),
              )
            )
          ))
        case other => throw new IllegalArgumentException(s"Unknown value :${other.toString}")
      }
  }

}
