package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition.HasData
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.TextProperty
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDefinition.NodeDefinition
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.instanceSyncRequestEncoder
import io.circe
import io.circe.Decoder
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.ZonedDateTime

@SuppressWarnings(
  Array(
    "org.wartremover.warts.NonUnitStatements"
  )
)
class InstanceSyncSerDerTest extends AnyWordSpec with Matchers {


  "Instance sync nodes Request" should {

    "be encoded to json" in {
      val viewReference = ViewReference("spaceId", "viewId", "version1")
      val hasData = HasData(Seq(viewReference))
      val request = InstanceSyncRequest(
        `with` = Map("sync" -> TableExpression(nodes = Option(NodesTableExpression(filter = Option(hasData))))),
        select = Map("sync" -> SelectExpression(sources = Seq(SourceSelector(source = viewReference, properties = List("*"))))))

      val requestAsJson = request.asJson
      val expectedJson =
        """{
          |  "with" : {
          |    "sync" : {
          |      "limit" : 1000,
          |      "nodes" : {
          |        "from" : null,
          |        "chainTo" : "destination",
          |        "direction" : "inwards",
          |        "filter" : {
          |          "hasData" : [
          |            {
          |              "type" : "view",
          |              "space" : "spaceId",
          |              "externalId" : "viewId",
          |              "version" : "version1"
          |            }
          |          ]
          |        },
          |        "through" : null
          |      }
          |    }
          |  },
          |  "select" : {
          |    "sync" : {
          |      "sources" : [
          |        {
          |          "source" : {
          |            "type" : "view",
          |            "space" : "spaceId",
          |            "externalId" : "viewId",
          |            "version" : "version1"
          |          },
          |          "properties" : [
          |            "*"
          |          ]
          |        }
          |      ]
          |    }
          |  }
          |}""".stripMargin

      requestAsJson.toString() should (be(expectedJson))

    }
  }

  "Instance sync edges Request" should {
    "be encoded to json" in {
      val viewReference = ViewReference("spaceId", "viewId", "version1")
      val hasData = HasData(Seq(viewReference))
      val request = InstanceSyncRequest(
        `with` = Map("sync" -> TableExpression(edges = Option(EdgeTableExpression(filter = Option(hasData))))),
        select = Map("sync" -> SelectExpression(sources = Seq(SourceSelector(source = viewReference, properties = List("*"))))),
        includeTyping = Option(true))

      val requestAsJson = request.asJson
      val expectedJson =
        """{
          |  "with" : {
          |    "sync" : {
          |      "limit" : 1000,
          |      "edges" : {
          |        "from" : null,
          |        "chainTo" : "destination",
          |        "maxDistance" : null,
          |        "direction" : "inwards",
          |        "filter" : {
          |          "hasData" : [
          |            {
          |              "type" : "view",
          |              "space" : "spaceId",
          |              "externalId" : "viewId",
          |              "version" : "version1"
          |            }
          |          ]
          |        },
          |        "nodeFilter" : null,
          |        "terminationFilter" : null
          |      }
          |    }
          |  },
          |  "select" : {
          |    "sync" : {
          |      "sources" : [
          |        {
          |          "source" : {
          |            "type" : "view",
          |            "space" : "spaceId",
          |            "externalId" : "viewId",
          |            "version" : "version1"
          |          },
          |          "properties" : [
          |            "*"
          |          ]
          |        }
          |      ]
          |    }
          |  },
          |  "includeTyping" : true
          |}""".stripMargin

      requestAsJson.toString() should (be(expectedJson))

    }
  }

  "Instance Sync Response" should {

    "be decoded from " in {
      val encoded =
        """{
          |    "items": {
          |        "sync1": [
          |            {
          |                "instanceType": "node",
          |                "space": "space-ext-id-1",
          |                "externalId": "space-name-1",
          |                "createdTime": 100,
          |                "lastUpdatedTime": 1000,
          |                "version": 10,
          |                "properties": {
          |                    "space-name-1": {
          |                        "view-or-container-id-1": {
          |                            "property-identifier11": "prop-id-1",
          |                            "property-identifier12": 102,
          |                            "property-identifier13": {
          |                                "space": "space-name-1",
          |                                "externalId": "extId1"
          |                            },
          |                            "property-identifier14": 5.1,
          |                            "property-identifier15": 1.0,
          |                            "property-identifier16": [
          |                                 1.0,
          |                                 5.1,
          |                                 100.0,
          |                                 0.1
          |                             ]
          |                        },
          |                        "view-or-container-id-2": {
          |                            "property-identifier21": true,
          |                            "property-identifier22": [
          |                                1.0,
          |                                3,
          |                                4
          |                            ]
          |                        }
          |                    },
          |                    "space-name-2": {
          |                        "view-or-container-id-3": {
          |                            "property-identifier31": "prop-id-2",
          |                            "property-identifier32": 103
          |                        },
          |                        "view-or-container-id-4": {
          |                            "property-identifier41": false,
          |                            "property-identifier42": [
          |                                "a",
          |                                "b",
          |                                "c"
          |                            ]
          |                        }
          |                    }
          |                }
          |            }
          |        ]
          |    },
          |    "nextCursor": {
          |        "sync1": "cursor-101"
          |    }
          |}""".stripMargin

      val expectedItems = List(
        NodeDefinition(
          externalId = "space-name-1",
          space = "space-ext-id-1",
          createdTime = 100,
          lastUpdatedTime = 1000,
          version = Option(10L),
          deletedTime = None,
          properties = Some(
            Map(
              "space-name-1" -> Map(
                "view-or-container-id-1" -> Map(
                  "property-identifier11" -> InstancePropertyValue.String("prop-id-1"),
                  "property-identifier12" -> InstancePropertyValue.Int32(102),
                  "property-identifier13" -> InstancePropertyValue.ViewDirectNodeRelation(
                    Some(DirectRelationReference(space = "space-name-1", externalId = "extId1"))
                  ),
                  "property-identifier14" -> InstancePropertyValue.Float64(5.1),
                  "property-identifier15" -> InstancePropertyValue.Float32(1.0f),
                  "property-identifier16" -> InstancePropertyValue.Float64List(List(1.0, 5.1, 100, 0.1))
                ),
                "view-or-container-id-2" -> Map(
                  "property-identifier21" -> InstancePropertyValue.Boolean(true),
                  "property-identifier22" -> InstancePropertyValue.Int32List(List(1, 3, 4))
                )
              ),
              "space-name-2" -> Map(
                "view-or-container-id-3" -> Map(
                  "property-identifier31" -> InstancePropertyValue.String("prop-id-2"),
                  "property-identifier32" -> InstancePropertyValue.Int32(103)
                ),
                "view-or-container-id-4" -> Map(
                  "property-identifier41" -> InstancePropertyValue.Boolean(false),
                  "property-identifier42" -> InstancePropertyValue.StringList(Seq("a", "b", "c"))
                )
              )
            )
          ),
          `type`=None,
        )
      )

      val items: Map[String, Seq[InstanceDefinition]] = Map("sync1" -> expectedItems)
      val cursors = Map[String, String]("sync1" -> "cursor-101")

      val actual: Either[circe.Error, InstanceSyncResponse] = parse(encoded).flatMap(Decoder[InstanceSyncResponse].decodeJson)
      Right(cursors) shouldBe actual.map(_.nextCursor)
      Right(Some(items)) shouldBe actual.map(_.items)
    }

    def validatePropValueJson(expectedValue: InstancePropertyValue, typePropertyDefinition: TypePropertyDefinition, value: String): Assertion = {
      val json ="""
          |{
          |    "items": {
          |        "sync1": [
          |            {
          |                "instanceType": "node",
          |                "space": "space-ext-id-1",
          |                "externalId": "space-name-1",
          |                "createdTime": 100,
          |                "lastUpdatedTime": 1000,
          |                "version": 10,
          |                "properties": {
          |                    "space-1": {
          |                        "view-1/v1": {
          |                            "testprop": """.stripMargin + value +"""
          |                         }
          |                     }
          |                 }
          |            }
          |        ]
          |    },
          |    "typing": {
          |         "sync1": {
          |            "space-1": {
          |                "view-1/v1": {
          |                    "testprop":
          |                          """.stripMargin + typePropertyDefinition.asJson.noSpaces +"""
          |
          |                 }
          |             }
          |          }
          |    },
          |    "nextCursor": {}
          |}""".stripMargin
      print (json)
      val actual: Either[circe.Error, InstanceSyncResponse] = parse(json).flatMap(Decoder[InstanceSyncResponse].decodeJson)

      actual match {
        case Left(value) => throw new Exception(value)
        case Right(value) => {
          val foundValue = value.items.flatMap(_.get("sync1"))
            .flatMap(_.headOption).flatMap(_.properties.flatMap(_.get("space-1")))
            .flatMap(_.get("view-1/v1")).flatMap(_.get("testprop"))
          foundValue shouldBe Some(expectedValue)
        }
      }
    }

    "decoded to be returned as Float32" in {
        val typeDefToTest = TypePropertyDefinition(
            Some(true),
            Some(false),
            Some(PropertyDefaultValue.Float32(0.0f)),
            None,
            None,
            PropertyType.PrimitiveProperty(PrimitivePropType.Float32, Some(false))
        )
        validatePropValueJson(InstancePropertyValue.Float32(1.0f), typeDefToTest, "1.0")
        validatePropValueJson(InstancePropertyValue.Float32(1.0f), typeDefToTest, "1")
        validatePropValueJson(InstancePropertyValue.Float32(1.0f), typeDefToTest, "1.0")
        validatePropValueJson(InstancePropertyValue.Float32(100.0f), typeDefToTest, "100.0")
        validatePropValueJson(InstancePropertyValue.Float32(0.1f), typeDefToTest, "0.1")

        // Not supported by circe
        //validatePropValueJson(InstancePropertyValue.Float32(Float.NegativeInfinity), typeDefToTest, "\"%s\"".format(Float.NegativeInfinity.toString) )
    }

    "decoded to be returned as StringList" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        Some(PropertyDefaultValue.Float64(0.0)),
        None,
        None,
        TextProperty(Some(true))
      )

      validatePropValueJson(InstancePropertyValue.StringList(Seq("a", "b", "c")), typeDefToTest, "[\"a\",\"b\",\"c\"]")
      validatePropValueJson(InstancePropertyValue.StringList(Seq("2023-01-02", "b", "c")), typeDefToTest, "[\"2023-01-02\",\"b\",\"c\"]")
      validatePropValueJson(InstancePropertyValue.StringList(Seq()), typeDefToTest, "[]")
    }

    "decoded to be returned as Float64List" in {
        val typeDefToTest = TypePropertyDefinition(
            Some(true),
            Some(false),
            Some(PropertyDefaultValue.Float64(0.0)),
            None,
            None,
            PropertyType.PrimitiveProperty(PrimitivePropType.Float64, Some(true))
        )
        validatePropValueJson(InstancePropertyValue.Float64List(Seq()), typeDefToTest, "[]")
        validatePropValueJson(InstancePropertyValue.Float64List(List(1.0, 1.0, 5.1, 100.0, 0.1)), typeDefToTest, "[1.0,1.0,5.1,100.0,0.1]")
        validatePropValueJson(InstancePropertyValue.Float64List(List(1.0, 1.0, 5.1, 100.0, 0.1)), typeDefToTest, "[1,1,5.1,100,0.1]")
        validatePropValueJson(InstancePropertyValue.Float64List(List(1.0, 1.0, 5.1, 100.0, 0.1)), typeDefToTest, "[1.0,1.0,5.1,100.0,0.1]")
        validatePropValueJson(InstancePropertyValue.Float64List(List(Double.MaxValue, 100.0, 0.1)),
          typeDefToTest, "[%s,%s,%s]".format(Double.MaxValue, 100.0, 0.1))
    }

    "decoded to be returned as Float64" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        Some(PropertyDefaultValue.Float64(0.0)),
        None,
        None,
        PropertyType.PrimitiveProperty(PrimitivePropType.Float64, Some(false))
      )
      validatePropValueJson(InstancePropertyValue.Float64(1.0), typeDefToTest, "1.0")
      validatePropValueJson(InstancePropertyValue.Float64(1.0), typeDefToTest, "1")
      validatePropValueJson(InstancePropertyValue.Float64(1.0), typeDefToTest, "1.0")
      validatePropValueJson(InstancePropertyValue.Float64(Double.MaxValue), typeDefToTest, Double.MaxValue.toString)
      validatePropValueJson(InstancePropertyValue.Float64(100.0), typeDefToTest, "100.0")
      validatePropValueJson(InstancePropertyValue.Float64(0.1), typeDefToTest, "0.1")
    }

    "decoded to be returned as Boolean" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        Some(PropertyDefaultValue.Boolean(false)),
        None,
        None,
        PropertyType.PrimitiveProperty(PrimitivePropType.Boolean, Some(false))
      )
      validatePropValueJson(InstancePropertyValue.Boolean(true), typeDefToTest, "true")
      validatePropValueJson(InstancePropertyValue.Boolean(false), typeDefToTest, "false")
    }

    "decoded to be returned as String" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        None,
        None,
        None,
        PropertyType.TextProperty(None)
      )
      validatePropValueJson(InstancePropertyValue.String(""), typeDefToTest, "\"\"")
      validatePropValueJson(InstancePropertyValue.String("1"),typeDefToTest,  "\"1\"")
      validatePropValueJson(InstancePropertyValue.String("2023-01-01"),typeDefToTest,"\"2023-01-01\"")
    }

    "decoded to be returned as TimeStamp" in {
        val typeDefToTest = TypePropertyDefinition(
            Some(true),
            Some(false),
            None,
            None,
            None,
            PropertyType.PrimitiveProperty(PrimitivePropType.Timestamp, Some(false))
        )
        validatePropValueJson(InstancePropertyValue.Timestamp(ZonedDateTime.parse("2023-01-01T01:01:01.001Z")),typeDefToTest, "\"2023-01-01T01:01:01.001Z\"")
        validatePropValueJson(InstancePropertyValue.Timestamp(ZonedDateTime.parse("2023-01-01T01:01:01.001+05:30")),
          typeDefToTest, "\"2023-01-01T01:01:01.001+05:30\"")
    }

    "decoded to be returned as Int64" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        None,
        None,
        None,
        PropertyType.PrimitiveProperty(PrimitivePropType.Int64, Some(false))
      )
      validatePropValueJson(InstancePropertyValue.Int64(1),typeDefToTest, "1.0")
      validatePropValueJson(InstancePropertyValue.Int64(1),typeDefToTest, "1")
      validatePropValueJson(InstancePropertyValue.Int64(-1),typeDefToTest, "-1.0")
      validatePropValueJson(InstancePropertyValue.Int64(0),typeDefToTest, "0")
      validatePropValueJson(InstancePropertyValue.Int64(Long.MaxValue),typeDefToTest, Long.MaxValue.toString)
      validatePropValueJson(InstancePropertyValue.Int64(Long.MinValue),typeDefToTest, Long.MinValue.toString)
    }

    "decoded to be returned as Int32" in {
      val typeDefToTest = TypePropertyDefinition(
        Some(true),
        Some(false),
        None,
        None,
        None,
        PropertyType.PrimitiveProperty(PrimitivePropType.Int32, Some(false))
      )
      validatePropValueJson(InstancePropertyValue.Int32(1),typeDefToTest, "1.0")
      validatePropValueJson(InstancePropertyValue.Int32(1),typeDefToTest, "1")
      validatePropValueJson(InstancePropertyValue.Int32(-1),typeDefToTest, "-1.0")
      validatePropValueJson(InstancePropertyValue.Int32(0),typeDefToTest, "0")
      validatePropValueJson(InstancePropertyValue.Int32(Integer.MAX_VALUE),typeDefToTest, Integer.MAX_VALUE.toString)
      validatePropValueJson(InstancePropertyValue.Int32(Integer.MIN_VALUE),typeDefToTest, Integer.MIN_VALUE.toString)
    }

    "be decoded with type information" in {
      val encoded =
        """{
          |    "items": {
          |        "sync1": [
          |            {
          |                "instanceType": "node",
          |                "space": "space-ext-id-1",
          |                "externalId": "space-name-1",
          |                "createdTime": 100,
          |                "lastUpdatedTime": 1000,
          |                "version": 10,
          |                "properties": {
          |                    "space-name-1": {
          |                        "view-or-container-id-1": {
          |                            "property-identifier11": "prop-id-1",
          |                            "property-identifier12": 102,
          |                            "property-identifier13": {
          |                                "space": "space-name-1",
          |                                "externalId": "extId1"
          |                            },
          |                            "property-identifier14": 5.1,
          |                            "property-identifier15": 1.0,
          |                            "property-identifier16": [
          |                                1.0,
          |                                1.0,
          |                                5.1,
          |                                100.0,
          |                                0.1
          |                            ]
          |                        },
          |                        "view-or-container-id-2": {
          |                            "property-identifier21": true,
          |                            "property-identifier22": [
          |                                1,
          |                                3,
          |                                4
          |                            ]
          |                        }
          |                    },
          |                    "space-name-2": {
          |                        "view-or-container-id-3": {
          |                            "property-identifier31": "prop-id-2",
          |                            "property-identifier32": 103
          |                        },
          |                        "view-or-container-id-4": {
          |                            "property-identifier41": false,
          |                            "property-identifier42": [
          |                                "2021-01-01",
          |                                "b",
          |                                "c"
          |                            ]
          |                        }
          |                    }
          |                }
          |            }
          |        ]
          |    },
          |    "typing": {
          |        "sync1": {
          |            "space-name-1": {
          |                "view-or-container-id-1": {
          |                    "property-identifier11": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": "default-str",
          |                        "description": "property-identifier11",
          |                        "name": "property-identifier11",
          |                        "type": {
          |                            "type": "text",
          |                            "list": false,
          |                            "collation": "ucs_basic"
          |                        }
          |                    },
          |                    "property-identifier12": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": 0,
          |                        "description": "property-identifier12",
          |                        "name": "property-identifier12",
          |                        "type": {
          |                            "type": "int64",
          |                            "list": false
          |                        }
          |                    },
          |                    "property-identifier13": {
          |                        "nullable": true,
          |                        "description": "property-identifier13",
          |                        "name": "property-identifier13",
          |                        "type": {
          |                            "type": "direct",
          |                            "container": {
          |                                "type": "container",
          |                                "space": "space-name-1",
          |                                "externalId": "extId1"
          |                            }
          |                        }
          |                    },
          |                    "property-identifier14": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "description": "property-identifier14",
          |                        "name": "property-identifier14",
          |                        "type": {
          |                            "type": "float64",
          |                            "list": false
          |                        }
          |                    },
          |                    "property-identifier15": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "description": "property-identifier15",
          |                        "name": "property-identifier15",
          |                        "type": {
          |                            "type": "float32",
          |                            "list": false
          |                        }
          |                    },
          |                    "property-identifier16": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "description": "property-identifier16",
          |                        "name": "property-identifier16",
          |                        "type": {
          |                            "type": "float64",
          |                            "list": true
          |                        }
          |                    }
          |                },
          |                "view-or-container-id-2": {
          |                    "property-identifier21": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": false,
          |                        "description": "property-identifier21",
          |                        "name": "property-identifier21",
          |                        "type": {
          |                            "type": "boolean",
          |                            "list": false
          |                        }
          |                    },
          |                    "property-identifier22": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "description": "property-identifier22",
          |                        "name": "property-identifier22",
          |                        "type": {
          |                            "type": "int64",
          |                            "list": true
          |                        }
          |                    }
          |                }
          |            },
          |            "space-name-2": {
          |                "view-or-container-id-3": {
          |                    "property-identifier31": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": "default-str",
          |                        "description": "property-identifier31",
          |                        "name": "property-identifier31",
          |                        "type": {
          |                            "type": "text",
          |                            "list": false,
          |                            "collation": "ucs_basic"
          |                        }
          |                    },
          |                    "property-identifier32": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": 0,
          |                        "description": "property-identifier32",
          |                        "name": "property-identifier32",
          |                        "type": {
          |                            "type": "int32",
          |                            "list": false
          |                        }
          |                    }
          |                },
          |                "view-or-container-id-4": {
          |                    "property-identifier41": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": false,
          |                        "description": "property-identifier41",
          |                        "name": "property-identifier41",
          |                        "type": {
          |                            "type": "boolean",
          |                            "list": false
          |                        }
          |                    },
          |                    "property-identifier42": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "description": "property-identifier42",
          |                        "name": "property-identifier42",
          |                        "type": {
          |                            "type": "text",
          |                            "list": true,
          |                            "collation": "ucs_basic"
          |                        }
          |                    },
          |                    "property-identifier43": {
          |                        "nullable": true,
          |                        "autoIncrement": false,
          |                        "defaultValue": "timeseries-43",
          |                        "description": "property-identifier43",
          |                        "name": "property-identifier43",
          |                        "type": {
          |                            "type": "timeseries"
          |                        }
          |                    }
          |                }
          |            }
          |        }
          |    },
          |    "nextCursor": {
          |        "sync1": "cursor-101"
          |    }
          |}""".stripMargin

      val expectedItems = List(
        NodeDefinition(
          externalId = "space-name-1",
          space = "space-ext-id-1",
          createdTime = 100,
          lastUpdatedTime = 1000,
          version = Option(10L),
          deletedTime = None,
          properties = Some(
            Map(
              "space-name-1" -> Map(
                "view-or-container-id-1" -> Map(
                  "property-identifier11" -> InstancePropertyValue.String("prop-id-1"),
                  "property-identifier12" -> InstancePropertyValue.Int64(102),
                  "property-identifier13" -> InstancePropertyValue.ViewDirectNodeRelation(
                    Some(DirectRelationReference(space = "space-name-1", externalId = "extId1"))
                  ),
                  "property-identifier14" -> InstancePropertyValue.Float64(5.1),
                  "property-identifier15" -> InstancePropertyValue.Float32(1.0f),
                  "property-identifier16" -> InstancePropertyValue.Float64List(List(1.0, 1.0, 5.1, 100, 0.1))
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
                  "property-identifier42" -> InstancePropertyValue.StringList(Seq("2021-01-01", "b", "c"))
                )
              )
            )
          ),
          `type`=None,
        )
      )

      val items: Map[String, Seq[InstanceDefinition]] = Map("sync1" -> expectedItems)
      val cursors = Map[String, String]("sync1" -> "cursor-101")

      val actual: Either[circe.Error, InstanceSyncResponse] = parse(encoded).flatMap(Decoder[InstanceSyncResponse].decodeJson)
      actual.map(_.nextCursor) shouldBe Right(cursors)
      actual.map(_.items) shouldBe Right(Some(items))
    }
  }
}
