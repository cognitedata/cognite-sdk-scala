package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.EnumValueMetadata
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.containers.ContainerReference
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDefinition.NodeDefinition
import io.circe
import io.circe.Decoder
import io.circe.parser.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.time.temporal.ChronoUnit

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Null"
  )
)
class InstancePropertySerDeTest extends AnyWordSpec with Matchers {

  "InstanceFilterResponse" should  {
    "ser/de" in {
      val createdTime: Long = Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli
      val lastUpdatedTime: Long = Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli

      val json: String =
        s"""{
           |  "items": [
           |    {
           |      "instanceType": "node",
           |      "space": "space-name-1",
           |      "externalId": "space-ext-id-1",
           |      "createdTime": ${createdTime.toString},
           |      "lastUpdatedTime": ${lastUpdatedTime.toString},
           |      "properties": {
           |        "space-name-1": {
           |          "view-or-container-id-1": {
           |            "property-identifier11": "prop-id-1",
           |            "property-identifier12": 102,
           |            "property-identifier13": {
           |              "space": "space-name-1",
           |              "externalId": "extId1"
           |            },
           |            "property-identifier14": "VAL1"
           |          },
           |          "view-or-container-id-2": {
           |            "property-identifier21": true,
           |            "property-identifier22": [1, 3, 4]
           |          }
           |        },
           |        "space-name-2": {
           |          "view-or-container-id-3": {
           |            "property-identifier31": "prop-id-2",
           |            "property-identifier32": 103
           |          },
           |          "view-or-container-id-4": {
           |            "property-identifier41": false,
           |            "property-identifier42": ["a", "b", "c"]
           |          }
           |        }
           |      },
           |      "type": {
           |        "space": "space-name-1",
           |        "externalId": "extId1"
           |      }
           |    }
           |  ],
           |  "typing": {
           |    "space-name-1": {
           |      "view-or-container-id-1": {
           |        "property-identifier11": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": "default-str",
           |          "description": "property-identifier11",
           |          "name": "property-identifier11",
           |          "type": {
           |            "type": "text",
           |            "list": false,
           |            "collation": "ucs_basic"
           |          }
           |        },
           |        "property-identifier12": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": 0,
           |          "description": "property-identifier12",
           |          "name": "property-identifier12",
           |          "type": {
           |            "type": "int64",
           |            "list": false
           |          }
           |        },
           |        "property-identifier13": {
           |          "nullable": true,
           |          "description": "property-identifier13",
           |          "name": "property-identifier13",
           |          "type": {
           |            "type": "direct",
           |            "container": {
           |              "type": "container",
           |              "space": "space-name-1",
           |              "externalId": "extId1"
           |            }
           |          }
           |        },
           |        "property-identifier14": {
           |          "nullable": true,
           |          "description": "property-identifier14",
           |          "name": "property-identifier14",
           |          "type": {
           |            "type": "enum",
           |            "unknownValue": "VAL2",
           |            "values": {
           |              "VAL1": {
           |                "name": "value1",
           |                "description": "value 1"
           |              },
           |              "VAL2": {}
           |            }
           |          }
           |        }
           |      },
           |      "view-or-container-id-2": {
           |        "property-identifier21": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": false,
           |          "description": "property-identifier21",
           |          "name": "property-identifier21",
           |          "type": {
           |            "type": "boolean",
           |            "list": false
           |          }
           |        },
           |        "property-identifier22": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "description": "property-identifier22",
           |          "name": "property-identifier22",
           |          "type": {
           |            "type": "int64",
           |            "list": true
           |          }
           |        }
           |      }
           |    },
           |    "space-name-2": {
           |      "view-or-container-id-3": {
           |        "property-identifier31": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": "default-str",
           |          "description": "property-identifier31",
           |          "name": "property-identifier31",
           |          "type": {
           |            "type": "text",
           |            "list": false,
           |            "collation": "ucs_basic"
           |          }
           |        },
           |        "property-identifier32": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": 0,
           |          "description": "property-identifier32",
           |          "name": "property-identifier32",
           |          "type": {
           |            "type": "int32",
           |            "list": false
           |          }
           |        }
           |      },
           |      "view-or-container-id-4": {
           |        "property-identifier41": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": false,
           |          "description": "property-identifier41",
           |          "name": "property-identifier41",
           |          "type": {
           |            "type": "boolean",
           |            "list": false
           |          }
           |        },
           |        "property-identifier42": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "description": "property-identifier42",
           |          "name": "property-identifier42",
           |          "type": {
           |            "type": "text",
           |            "list": true,
           |            "collation": "ucs_basic"
           |          }
           |        },
           |        "property-identifier43": {
           |          "nullable": true,
           |          "autoIncrement": false,
           |          "defaultValue": "timeseries-43",
           |          "description": "property-identifier43",
           |          "name": "property-identifier43",
           |          "type": {
           |            "type": "timeseries"
           |          }
           |        }
           |      }
           |    }
           |  },
           |  "nextCursor": "cursor-101"
           |}""".stripMargin

      val instanceFilterResponse: InstanceFilterResponse = InstanceFilterResponse(
        Seq(
          NodeDefinition(
            "space-name-1",
            "space-ext-id-1",
            createdTime,
            lastUpdatedTime,
            deletedTime = None,
            version = None,
            properties = Some(
              Map(
                "space-name-1" -> Map(
                  "view-or-container-id-1" -> Map(
                    "property-identifier11" -> InstancePropertyValue.String("prop-id-1"),
                    "property-identifier12" -> InstancePropertyValue.Int64(102),
                    "property-identifier13" -> InstancePropertyValue.ViewDirectNodeRelation(
                      Some(DirectRelationReference(space = "space-name-1", externalId = "extId1"))
                    ),
                    "property-identifier14" -> InstancePropertyValue.Enum("VAL1")
                  ),
                  "view-or-container-id-2" -> Map(
                    "property-identifier21" -> InstancePropertyValue.Boolean(true),
                    "property-identifier22" -> InstancePropertyValue.Int64List(List(1, 3, 4))
                  )
                ),
                "space-name-2" -> Map(
                  "view-or-container-id-3" -> Map(
                    "property-identifier31" -> InstancePropertyValue.String("prop-id-2"),
                    "property-identifier32" -> InstancePropertyValue.Int32(103)
                  ),
                  "view-or-container-id-4" -> Map(
                    "property-identifier41" -> InstancePropertyValue.Boolean(false),
                    "property-identifier42" -> InstancePropertyValue.StringList(List("a", "b", "c"))
                  )
                )
              )
            ),
            Some(DirectRelationReference(space = "space-name-1", externalId = "extId1"))
          )
        ),
        typing = Some(
          Map(
            "space-name-1" -> Map(
              "view-or-container-id-1" -> Map(
                "property-identifier11" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.String("default-str")),
                  Some("property-identifier11"),
                  Some("property-identifier11"),
                  PropertyType.TextProperty(Some(false), Some("ucs_basic"))
                ),
                "property-identifier12" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.Int64(0)),
                  Some("property-identifier12"),
                  Some("property-identifier12"),
                  PropertyType.PrimitiveProperty(PrimitivePropType.Int64, Some(false))
                ),
                "property-identifier13" -> TypePropertyDefinition(
                  Some(true),
                  None,
                  None,
                  Some("property-identifier13"),
                  Some("property-identifier13"),
                  PropertyType.DirectNodeRelationProperty(Some(ContainerReference("space-name-1", "extId1")), None, None)
                ),
                "property-identifier14" -> TypePropertyDefinition(
                  Some(true),
                  None,
                  None,
                  Some("property-identifier14"),
                  Some("property-identifier14"),
                  PropertyType.EnumProperty(
                    values = Map(
                      "VAL1" -> EnumValueMetadata(Some("value1"), Some("value 1")),
                      "VAL2" -> EnumValueMetadata(None, None)
                    ),
                    unknownValue = Some("VAL2")
                  )
                )
              ),
              "view-or-container-id-2" -> Map(
                "property-identifier21" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.Boolean(false)),
                  Some("property-identifier21"),
                  Some("property-identifier21"),
                  PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(false))
                ),
                "property-identifier22" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  None,
                  Some("property-identifier22"),
                  Some("property-identifier22"),
                  PropertyType.PrimitiveProperty(PrimitivePropType.Int64, Some(true))
                )
              )
            ),
            "space-name-2" -> Map(
              "view-or-container-id-3" -> Map(
                "property-identifier31" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.String("default-str")),
                  Some("property-identifier31"),
                  Some("property-identifier31"),
                  PropertyType.TextProperty(Some(false), Some("ucs_basic"))
                ),
                "property-identifier32" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.Int32(0)),
                  Some("property-identifier32"),
                  Some("property-identifier32"),
                  PropertyType.PrimitiveProperty(PrimitivePropType.Int32, Some(false))
                )
              ),
              "view-or-container-id-4" -> Map(
                "property-identifier41" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.Boolean(false)),
                  Some("property-identifier41"),
                  Some("property-identifier41"),
                  PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(false))
                ),
                "property-identifier42" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  None,
                  Some("property-identifier42"),
                  Some("property-identifier42"),
                  PropertyType.TextProperty(Some(true), Some("ucs_basic"))
                ),
                "property-identifier43" -> TypePropertyDefinition(
                  Some(true),
                  Some(false),
                  Some(PropertyDefaultValue.String("timeseries-43")),
                  Some("property-identifier43"),
                  Some("property-identifier43"),
                  PropertyType.TimeSeriesReference()
                )
              )
            )
          )
        ),
        Some("cursor-101")
      )

      val actual: Either[circe.Error, InstanceFilterResponse] = parse(json).flatMap(Decoder[InstanceFilterResponse].decodeJson)

      actual shouldBe Right(instanceFilterResponse)
    }

  }
}
