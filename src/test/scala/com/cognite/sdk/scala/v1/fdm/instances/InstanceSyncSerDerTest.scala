package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.DirectRelationReference
import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition.HasData
import com.cognite.sdk.scala.v1.fdm.instances.InstanceDefinition.NodeDefinition
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.instanceSyncRequestEncoder
import io.circe
import io.circe.Decoder
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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
        select = Map("sync" -> SelectExpression(sources = Seq(SourceSelector(source = viewReference, properties = List("*"))))))

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
          |  }
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
          |                            "property-identifier15": 1.0
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
          )
        )
      )

      val items: Map[String, Seq[InstanceDefinition]] = Map("sync1" -> expectedItems)
      val cursors = Map[String, String]("sync1" -> "cursor-101")

      val actual: Either[circe.Error, InstanceSyncResponse] = parse(encoded).flatMap(Decoder[InstanceSyncResponse].decodeJson)
      Right(Some(cursors)) shouldBe actual.map(_.nextCursor)
      Right(Some(items)) shouldBe actual.map(_.items)
    }
  }
}
