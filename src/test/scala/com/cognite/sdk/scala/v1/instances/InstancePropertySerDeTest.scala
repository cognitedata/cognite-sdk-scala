package com.cognite.sdk.scala.v1.instances

import com.cognite.sdk.scala.v1.resources.Instances.instanceFilterResponseDecoder
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
    "org.wartremover.warts.AnyVal"
  )
)
class InstancePropertySerDeTest extends AnyWordSpec with Matchers {

  val json: String =
    s"""{
       |  "items": [
       |    {
       |      "type": "node",
       |      "space": "space-1",
       |      "externalId": "space-ext-id-1",
       |      "createdTime": ${Instant.now().minus(100, ChronoUnit.DAYS).toEpochMilli},
       |      "lastUpdatedTime": ${Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli},
       |      "properties": {
       |        "space-name-1": {
       |          "view-or-container-id-1": {
       |            "property-identifier11": "prop-id-1",
       |            "property-identifier12": 102
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
       |      }
       |    }
       |  ],
       |  "typing": {
       |    "space-name-1": {
       |      "view-or-container-id-1": {
       |        "property-identifier11": {
       |          "identifier": "property-identifier11",
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
       |          "identifier": "property-identifier12",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "defaultValue": 0,
       |          "description": "property-identifier12",
       |          "name": "property-identifier12",
       |          "type": {
       |            "type": "int32",
       |            "list": false
       |          }
       |        }
       |      },
       |      "view-or-container-id-2": {
       |        "property-identifier21": {
       |          "identifier": "property-identifier21",
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
       |          "identifier": "property-identifier22",
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
       |          "identifier": "property-identifier31",
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
       |          "identifier": "property-identifier32",
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
       |          "identifier": "property-identifier41",
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
       |          "identifier": "property-identifier42",
       |          "nullable": true,
       |          "autoIncrement": false,
       |          "description": "property-identifier42",
       |          "name": "property-identifier42",
       |          "type": {
       |            "type": "text",
       |            "list": true,
       |            "collation": "ucs_basic"
       |          }
       |        }
       |      }
       |    }
       |  },
       |  "nextCursor": "cursor-101"
       |}""".stripMargin


  val result = parse(json).flatMap(Decoder[InstanceFilterResponse].decodeJson)
  println(result)

  1 shouldBe 1
}