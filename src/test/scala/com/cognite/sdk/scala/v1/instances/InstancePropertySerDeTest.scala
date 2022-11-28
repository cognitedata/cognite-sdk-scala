package com.cognite.sdk.scala.v1.instances

import io.circe.Decoder
import io.circe.parser.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal"
  )
)
class InstancePropertySerDeTest extends AnyWordSpec with Matchers {

  import com.cognite.sdk.scala.v1.resources.Instances._

  val json: String =
    s"""{
       |  "items": [
       |    {
       |      "type": "node",
       |      "space": "string",
       |      "externalId": "string",
       |      "createdTime": 0,
       |      "lastUpdatedTime": 0,
       |      "properties": {
       |        "space-name1": {
       |          "view-or-container-identifier1": {
       |            "property-identifier1": "string",
       |            "property-identifier2": "string"
       |          },
       |          "view-or-container-identifier2": {
       |            "property-identifier1": "string",
       |            "property-identifier2": "string"
       |          }
       |        },
       |        "space-name2": {
       |          "view-or-container-identifier1": {
       |            "property-identifier1": "string",
       |            "property-identifier2": "string"
       |          },
       |          "view-or-container-identifier2": {
       |            "property-identifier1": "string",
       |            "property-identifier2": "string"
       |          }
       |        }
       |      }
       |    }
       |  ],
       |  "typing": {
       |    "space-name1": {
       |      "view-or-container-external-id1": {
       |        "property-identifier1": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        },
       |        "property-identifier2": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        }
       |      },
       |      "view-or-container-external-id2": {
       |        "property-identifier1": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        },
       |        "property-identifier2": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        }
       |      }
       |    },
       |    "space-name2": {
       |      "view-or-container-external-id1": {
       |        "property-identifier1": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        },
       |        "property-identifier2": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        }
       |      },
       |      "view-or-container-external-id2": {
       |        "property-identifier1": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        },
       |        "property-identifier2": {
       |          "identifier": "string",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": "string",
       |          "description": "string",
       |          "name": "string",
       |          "type": {
       |            "type": "text",
       |            "list": false,
       |            "collation": "ucs_basic"
       |          }
       |        }
       |      }
       |    }
       |  },
       |  "nextCursor": "string"
       |}""".stripMargin


  val result = parse(json).flatMap(Decoder[InstanceFilterResponse].decodeJson)
  println(result)
  1 shouldBe 1
}
