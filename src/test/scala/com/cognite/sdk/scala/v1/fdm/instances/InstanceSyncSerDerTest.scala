package com.cognite.sdk.scala.v1.fdm.instances

import com.cognite.sdk.scala.v1.fdm.common.filters.FilterDefinition.HasData
import com.cognite.sdk.scala.v1.fdm.views.ViewReference
import com.cognite.sdk.scala.v1.resources.fdm.instances.Instances.instanceSyncRequestEncoder
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
          |  },
          |  "includeTyping" : true
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
}
