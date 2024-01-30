package com.cognite.sdk.scala.v1.fdm

import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{ContainerPropertyDefinition, CorePropertyDefinition, ViewCorePropertyDefinition}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType._
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.common.sources.SourceReference
import com.cognite.sdk.scala.v1.fdm.common.{DirectRelationReference, Usage}
import com.cognite.sdk.scala.v1.fdm.containers._
import com.cognite.sdk.scala.v1.fdm.instances.NodeOrEdgeCreate.{EdgeWrite, NodeWrite}
import com.cognite.sdk.scala.v1.fdm.instances.{EdgeOrNodeData, InstancePropertyValue, NodeOrEdgeCreate}
import io.circe.{Json, JsonObject}

import java.time.{LocalDate, LocalDateTime, ZoneId}
import scala.util.Random

object Utils {
  val SpaceExternalId = "testSpaceScalaSdk1"
  val DirectNodeRelationContainerExtId = "sdkTestNodeContainerForDirectRelation6"
  val DirectNodeRelationViewExtId = "sdkTestNodeViewForDirectRelation6"
  val ViewVersion = "v1"

  val AllContainerPropertyTypes: List[PropertyType] = List[PropertyType](
    TextProperty(),
    PrimitiveProperty(`type` = PrimitivePropType.Boolean),
    PrimitiveProperty(`type` = PrimitivePropType.Float32),
    PrimitiveProperty(`type` = PrimitivePropType.Float64),
    PrimitiveProperty(`type` = PrimitivePropType.Int32),
    PrimitiveProperty(`type` = PrimitivePropType.Int64),
    PrimitiveProperty(`type` = PrimitivePropType.Timestamp),
    PrimitiveProperty(`type` = PrimitivePropType.Date),
    PrimitiveProperty(`type` = PrimitivePropType.Json),
    TextProperty(list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Boolean, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Float32, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Float64, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Int32, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Int64, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Timestamp, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Date, list = Some(true)),
    PrimitiveProperty(`type` = PrimitivePropType.Json, list = Some(true)),
    DirectNodeRelationProperty(
      container = None,
      source = None),
    TimeSeriesReference(list = Some(false)),
    FileReference(list = Some(false)),
    SequenceReference(list = Some(false)),
    TimeSeriesReference(list = Some(true)),
    FileReference(list = Some(true)),
    SequenceReference(list = Some(true))
  )

  val AllPropertyDefaultValues: List[PropertyDefaultValue] = List(
    PropertyDefaultValue.String("abc"),
    PropertyDefaultValue.Boolean(true),
    PropertyDefaultValue.Int32(101),
    PropertyDefaultValue.Int64(Long.MaxValue),
    PropertyDefaultValue.Float32(101.1f),
    PropertyDefaultValue.Float64(Double.MaxValue),
    PropertyDefaultValue.TimeSeriesReference("defaultTimeSeriesExtId"),
    PropertyDefaultValue.FileReference("defaultFileExtId"),
    PropertyDefaultValue.SequenceReference("defaultSequenceExtId"),
    PropertyDefaultValue.Object(
      Json.fromJsonObject(
        JsonObject.fromMap(
          Map(
            "a" -> Json.fromString("a"),
            "b" -> Json.fromInt(1),
            "c" -> Json.fromFloatOrString(2.1f)
          )
        )
      )
    )
  )

  def toViewPropertyDefinition(
                                containerPropDef: ContainerPropertyDefinition,
                                containerRef: Option[ContainerReference],
                                containerPropertyIdentifier: Option[String]): ViewCorePropertyDefinition =
    ViewCorePropertyDefinition(
      nullable = containerPropDef.nullable,
      autoIncrement = containerPropDef.autoIncrement,
      defaultValue = containerPropDef.defaultValue,
      description = containerPropDef.description,
      name = containerPropDef.name,
      `type` = containerPropDef.`type`,
      container = containerRef,
      containerPropertyIdentifier = containerPropertyIdentifier
    )

  // scalastyle:off cyclomatic.complexity method.length
  def createAllPossibleContainerPropCombinations: Map[String, ContainerPropertyDefinition] = {
    val boolOptions = List(
      true,
      false
    )

    (for {
      p <- AllContainerPropertyTypes
      nullable <- boolOptions
      withDefault <- boolOptions
    } yield {
      val defaultValue = propertyDefaultValueForPropertyType(p, withDefault)

      val autoIncrementApplicableProp = p match {
        case PrimitiveProperty(PrimitivePropType.Int32, None | Some(false)) => true
        case PrimitiveProperty(PrimitivePropType.Int64, None | Some(false)) => true
        case _ => false
      }

      val autoIncrement = autoIncrementApplicableProp && !nullable && !withDefault

      val alwaysNullable = p match {
        case _: DirectNodeRelationProperty => true
        case _ => false
      }
      val nullability = alwaysNullable || nullable

      val nameComponents = Vector(
        p match {
          case p: PrimitiveProperty => p.`type`.productPrefix
          case _ => p.getClass.getSimpleName
        },
        if (p.isList) "List" else "NonList",
        if (autoIncrementApplicableProp) {
          if (autoIncrement) "WithAutoIncrement" else "WithoutAutoIncrement"
        } else { "" },
        if (defaultValue.nonEmpty) "WithDefaultValue" else "WithoutDefaultValue",
        if (nullability) "Nullable" else "NonNullable"
      )

      s"${nameComponents.mkString("")}" -> ContainerPropertyDefinition(
        nullable = Some(nullability),
        autoIncrement = Some(autoIncrement),
        defaultValue = defaultValue,
        description = Some(s"Test ${nameComponents.mkString(" ")} Description"),
        name = Some(s"Test-${nameComponents.mkString("-")}-Name"),
        `type` = p
      )
    }).toMap
  }
  // scalastyle:on cyclomatic.complexity method.length

  def createAllPossibleViewPropCombinations: Map[String, ViewCorePropertyDefinition] =
    createAllPossibleContainerPropCombinations.map {
      case (key, prop) => key -> toViewPropertyDefinition(prop, None, None)
    }

  def createTestContainer(
                           space: String,
                           containerExternalId: String,
                           usage: Usage
                         ): ContainerCreateDefinition = {
    val allPossibleProperties: Map[String, ContainerPropertyDefinition] =
      createAllPossibleContainerPropCombinations
//    val allPossiblePropertyKeys = allPossibleProperties.keys.toList
//
//    val constraints: Map[String, ContainerConstraint] = Map(
//      "uniqueConstraint" -> ContainerConstraint.UniquenessConstraint(
//        allPossiblePropertyKeys.take(5)
//      )
//    )
//
//    val indexes: Map[String, IndexDefinition] = Map(
//      "index1" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.take(2)),
//      "index2" -> IndexDefinition.BTreeIndexDefinition(allPossiblePropertyKeys.slice(5, 7))
//    )

    val containerToCreate = ContainerCreateDefinition(
      space = space,
      externalId = containerExternalId,
      name = Some(s"Test-${usage.productPrefix}-Container-$containerExternalId-Name"),
      description = Some(s"Test ${usage.productPrefix} Container $containerExternalId Description"),
      usedFor = Some(usage),
      properties = allPossibleProperties,
      constraints = None,
      indexes = None
    )

    containerToCreate
  }

  def createInstancePropertyForContainerProperty(
                                                  propName: String,
                                                  containerPropType: PropertyType
                                                ): InstancePropertyValue = {
    containerPropType match {
      case DirectNodeRelationProperty(container, _) =>
        val ref = container.map(r => DirectRelationReference(r.space, s"${r.externalId}Instance"))
        InstancePropertyValue.ViewDirectNodeRelation(ref)
      case p if p.isList => listContainerPropToInstanceProperty(propName, p)
      case p => nonListContainerPropToInstanceProperty(propName, p)
    }
  }

  def createNodeWriteData(space: String,
                          nodeExternalId: String,
                          sourceRef: SourceReference,
                          propsMap: Map[String, CorePropertyDefinition]): NodeWrite = {
    val instanceValuesForProps = propsMap.map {
      case (propName, prop) =>
        propName -> createInstancePropertyForContainerProperty(propName, prop.`type`)
    }
//    val (nullables, nonNullables) = instanceValuesForProps.partition {
//      case (propName, _) =>
//        propsMap(propName).nullable.getOrElse(true)
//    }

//    val nodeData = if (nullables.isEmpty) {
//      List(toInstanceData(sourceRef, nonNullables))
//    } else
//    {
      val withAllProps = toInstanceData(sourceRef, instanceValuesForProps)
//      val withRequiredProps = toInstanceData(sourceRef, nonNullables)
//      val withEachNonRequired = nullables.map {
//        case (propName, propVal) =>
//          toInstanceData(sourceRef, nonNullables + (propName -> propVal))
//      }
//      List(withAllProps, withRequiredProps) ++ withEachNonRequired
//      withAllProps
//    }

    NodeWrite(
      space,
      nodeExternalId,
      Some(Seq(withAllProps))
    )
  }

  def createEdgeWriteData(space: String,
                          edgeExternalId: String,
                          sourceRef: SourceReference,
                          propsMap: Map[String, CorePropertyDefinition],
                          startNode: DirectRelationReference,
                          endNode: DirectRelationReference
                         ): EdgeWrite = {
    val instanceValuesForProps = propsMap.map {
      case (propName, prop) =>
        propName -> createInstancePropertyForContainerProperty(propName, prop.`type`)
    }
    val (nullables, nonNullables) = instanceValuesForProps.partition {
      case (propName, _) =>
        propsMap(propName).nullable.getOrElse(true)
    }

    val nodeData = if (nullables.isEmpty) {
      List(toInstanceData(sourceRef, nonNullables))
    } else {
      val withAllProps = toInstanceData(sourceRef, instanceValuesForProps)
      val withRequiredProps = toInstanceData(sourceRef, nonNullables)
      val withEachNonRequired = nullables.map {
        case (propName, propVal) =>
          toInstanceData(sourceRef, nonNullables + (propName -> propVal))
      }
      val allPropsMaps =
        withAllProps.properties.toList ++
        withRequiredProps.properties.toList ++
        withEachNonRequired.flatMap(_.properties.toList)
      List(EdgeOrNodeData(
        source = sourceRef,
        properties = Some(allPropsMaps.flatMap(_.toList).toMap).filterNot(_.isEmpty)
      ))
    }

    EdgeWrite(
      `type` = DirectRelationReference(space, edgeExternalId),
      space = space,
      externalId = edgeExternalId,
      startNode = startNode,
      endNode = endNode,
      sources = Some(nodeData)
    )
  }

  def createEdgeOrNodeWriteData(
                                 space: String,
                                 nodeOrEdgeExternalId: String,
                                 usage: Usage,
                                 sourceRef: SourceReference,
                                 propsMap: Map[String, CorePropertyDefinition],
                                 startNode: DirectRelationReference,
                                 endNode: DirectRelationReference
                         ): NodeOrEdgeCreate =
    usage match {
      case u@(Usage.Node | Usage.Edge) =>
        throw new IllegalArgumentException(s"${sourceRef.toString} supports only: ${u.productPrefix}s. Should support both nodes & edges!")
      case _ =>
        val instanceValuesForProps = propsMap.map {
          case (propName, prop) =>
            propName -> createInstancePropertyForContainerProperty(propName, prop.`type`)
        }
        val (nullables, nonNullables) = instanceValuesForProps.partition {
          case (propName, _) =>
            propsMap(propName).nullable.getOrElse(true)
        }

        val nodeData = if (nullables.isEmpty) {
          List(toInstanceData(sourceRef, nonNullables))
        } else {
          val withAllProps = toInstanceData(sourceRef, instanceValuesForProps)
          val withRequiredProps = toInstanceData(sourceRef, nonNullables)
          val withEachNonRequired = nullables.map {
            case (propName, propVal) =>
              toInstanceData(sourceRef, nonNullables + (propName -> propVal))
          }
          val allPropsMaps =
            withAllProps.properties.toList ++
              withRequiredProps.properties.toList ++
              withEachNonRequired.flatMap(_.properties.toList)
          List(EdgeOrNodeData(
            source = sourceRef,
            properties = Some(allPropsMaps.flatMap(_.toList).toMap).filterNot(_.isEmpty)
          ))
        }

        EdgeWrite(
          `type` = DirectRelationReference(space, nodeOrEdgeExternalId),
          space = space,
          externalId = nodeOrEdgeExternalId,
          startNode = startNode,
          endNode = endNode,
          sources = Some(nodeData)
        )
    }

  private val jsonListExamle = InstancePropertyValue.ObjectList(
    List(
      Json.fromJsonObject(
        JsonObject.fromMap(
          Map(
            "a" -> Json.fromString("a"),
            "b" -> Json.fromInt(1),
            "c" -> Json.fromBoolean(true)
          )
        )
      ),
      Json.fromJsonObject(
        JsonObject.fromMap(
          Map(
            "a" -> Json.fromString("b"),
            "b" -> Json.fromInt(1),
            "c" -> Json.fromBoolean(false),
            "d" -> Json.fromDoubleOrString(1.56)
          )
        )
      )
    )
  )

  // scalastyle:off cyclomatic.complexity
  private def listContainerPropToInstanceProperty(
                                                   propName: String,
                                                   propertyType: PropertyType
                                                 ): InstancePropertyValue =
    propertyType match {
      case PropertyType.TextProperty(Some(true), _) =>
        InstancePropertyValue.StringList(List(s"${propName}Value1", s"${propName}Value2"))
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(true)) =>
        InstancePropertyValue.BooleanList(List(true, false, true, false))
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, Some(true)) =>
        InstancePropertyValue.Int32List((1 to 10).map(_ => Random.nextInt(10000)).toList)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, Some(true)) =>
        InstancePropertyValue.Int64List((1 to 10).map(_ => Random.nextLong()).toList)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float32, Some(true)) =>
        InstancePropertyValue.Float32List((1 to 10).map(_ => Random.nextFloat()).toList)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float64, Some(true)) =>
        InstancePropertyValue.Float64List((1 to 10).map(_ => Random.nextDouble()).toList)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, Some(true)) =>
        InstancePropertyValue.DateList(
          (1 to 10).toList.map(i => LocalDate.now().minusDays(i.toLong))
        )
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(true)) =>
        InstancePropertyValue.TimestampList(
          (1 to 10).toList.map(i => LocalDateTime.now().minusDays(i.toLong).atZone(ZoneId.of("UTC")))
        )
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, Some(true)) =>
        jsonListExamle
      case PropertyType.SequenceReference(Some(true)) =>
        InstancePropertyValue.SequenceReferenceList(List("seq1", "seq2"))
      case PropertyType.FileReference(Some(true)) =>
        InstancePropertyValue.FileReferenceList(List("file1", "file2", "file3"))
      case PropertyType.TimeSeriesReference(Some(true)) =>
        InstancePropertyValue.TimeSeriesReferenceList(List("ts1", "ts2", "ts3", "ts4"))
      case other => throw new IllegalArgumentException(s"Unknown value :${other.toString}")
    }
  // scalastyle:on cyclomatic.complexity

  // scalastyle:off cyclomatic.complexity
  private def nonListContainerPropToInstanceProperty(
                                                      propName: String,
                                                      propertyType: PropertyType
                                                    ): InstancePropertyValue =
    propertyType match {
      case PropertyType.TextProperty(None | Some(false), _) =>
        InstancePropertyValue.String(s"${propName}Value")
      case PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, _) =>
        InstancePropertyValue.Boolean(false)
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int32, None | Some(false)) =>
        InstancePropertyValue.Int32(Random.nextInt(10000))
      case PropertyType.PrimitiveProperty(PrimitivePropType.Int64, None | Some(false)) =>
        InstancePropertyValue.Int64(Random.nextLong())
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float32, None | Some(false)) =>
        InstancePropertyValue.Float32(Random.nextFloat())
      case PropertyType.PrimitiveProperty(PrimitivePropType.Float64, None | Some(false)) =>
        InstancePropertyValue.Float64(Random.nextDouble())
      case PropertyType.PrimitiveProperty(PrimitivePropType.Date, None | Some(false)) =>
        InstancePropertyValue.Date(LocalDate.now().minusDays(Random.nextInt(30).toLong))
      case PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, None | Some(false)) =>
        InstancePropertyValue.Timestamp(LocalDateTime.now().minusDays(Random.nextInt(30).toLong).atZone(ZoneId.of("UTC")))
      case PropertyType.PrimitiveProperty(PrimitivePropType.Json, None | Some(false)) =>
        InstancePropertyValue.Object(
          Json.fromJsonObject(
            JsonObject.fromMap(
              Map(
                "a" -> Json.fromString("a"),
                "b" -> Json.fromInt(1),
                "c" -> Json.fromBoolean(true)
              )
            )
          )
        )
      case _: PropertyType.TimeSeriesReference => InstancePropertyValue.TimeSeriesReference(s"$propName-reference")
      case _: PropertyType.FileReference => InstancePropertyValue.FileReference(s"$propName-reference")
      case _: PropertyType.SequenceReference => InstancePropertyValue.SequenceReference(s"$propName-reference")
      case other => throw new IllegalArgumentException(s"Unknown value :${other.toString}")
    }
  // scalastyle:on cyclomatic.complexity

  private def toInstanceData(ref: SourceReference, instancePropertyValues: Map[String, InstancePropertyValue]) =
    EdgeOrNodeData(
      source = ref,
      properties = Some(instancePropertyValues.mapValues(v => Some(v)).toMap)
    )

  // scalastyle:off cyclomatic.complexity
  private def propertyDefaultValueForPropertyType(
                                                   p: PropertyType,
                                                   withDefault: Boolean
                                                 ): Option[PropertyDefaultValue] =
    if (withDefault && !p.isList) {
      p match {
        case TextProperty(_, _) => Some(PropertyDefaultValue.String("defaultTextValue"))
        case PrimitiveProperty(PrimitivePropType.Boolean, _) =>
          Some(PropertyDefaultValue.Boolean(false))
        case PrimitiveProperty(PrimitivePropType.Float32, _) =>
          Some(PropertyDefaultValue.Float32(1.2f))
        case PrimitiveProperty(PrimitivePropType.Float64, _) =>
          Some(PropertyDefaultValue.Float64(1.21))
        case PrimitiveProperty(PrimitivePropType.Int32, _) => Some(PropertyDefaultValue.Int32(1))
        case PrimitiveProperty(PrimitivePropType.Int64, _) => Some(PropertyDefaultValue.Int64(12L))
        case PrimitiveProperty(PrimitivePropType.Timestamp, _) =>
          Some(
            PropertyDefaultValue.String(
              LocalDateTime
                .now()
                .atZone(ZoneId.of("UTC"))
                .format(InstancePropertyValue.Timestamp.formatter)
            )
          )
        case PrimitiveProperty(PrimitivePropType.Date, _) =>
          Some(
            PropertyDefaultValue.String(
              LocalDate.now().format(InstancePropertyValue.Date.formatter)
            )
          )
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
        case _: DirectNodeRelationProperty => None
        case _: TimeSeriesReference => Some(PropertyDefaultValue.TimeSeriesReference("defaultTimeSeriesExternalId"))
        case _: FileReference => Some(PropertyDefaultValue.FileReference("defaultFileExternalId"))
        case _: SequenceReference => Some(PropertyDefaultValue.SequenceReference("defaultSequenceExternalId"))
      }
    } else {
      None
    }
  // scalastyle:on cyclomatic.complexity

}
