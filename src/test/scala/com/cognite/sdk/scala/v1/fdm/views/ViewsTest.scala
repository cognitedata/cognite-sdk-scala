// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.common.RetryWhile
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefaultValue.{Int32, TimeSeriesReference}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.{
  ContainerPropertyDefinition,
  ReverseDirectRelationConnection,
  ThroughReference,
  ViewCorePropertyDefinition
}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType.PrimitiveProperty
import com.cognite.sdk.scala.v1.fdm.common.properties.ReverseDirectRelationConnectionType.MultiReverseDirectRelation
import com.cognite.sdk.scala.v1.fdm.common.properties.{PrimitivePropType, PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.common.{DataModelReference, Usage}
import com.cognite.sdk.scala.v1.fdm.containers._
import com.cognite.sdk.scala.v1.fdm.views.ViewPropertyCreateDefinition.{CreateConnectionDefinition, CreateViewProperty}
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, SpaceCreateDefinition}
import io.circe.Json
import org.scalatest.BeforeAndAfterAll

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AsInstanceOf"
  )
)
class ViewsTest extends CommonDataModelTestHelper with RetryWhile with BeforeAndAfterAll {
  private val spaceName = "test-space-scala-sdk"
  private val containerNamePrim = "scala sdk container prim"
  private val containerPrimitiveExternalId = "scala_sdk_container_primitive"

  private val containerNameList = "scala sdk container list"
  private val containerListExternalId = "scala_sdk_container_list"

  private val containerPropertyInt = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.Int32(1)),
    description = Some("Prop int32"),
    name = Some("Prop int32"),
    `type` = PrimitiveProperty(PrimitivePropType.Int32)
  )

  private val containerPropertyText = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.String("toto")),
    description = Some("Prop text"),
    name = Some("Prop text"),
    `type` = PropertyType.TextProperty()
  )

  private val containerTimeSeriesProperty = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.String("flux-capacitor-levels")),
    description = Some("defaultFlux1"),
    name = Some("DeLorean flux capacitor levels"),
    `type` = PropertyType.TimeSeriesReference()
  )

  private val containerPropertyDirectRelation = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.Object(Json.Null)),
    description = Some("Prop text"),
    name = Some("Prop text"),
    `type` = PropertyType.DirectNodeRelationProperty(None, None, None)
  )

  private val containerPrimitive = ContainerCreateDefinition(
    space = spaceName,
    externalId = containerPrimitiveExternalId,
    name = Some(containerNamePrim),
    description = Some("this is a container of primitive types"),
    usedFor = Some(Usage.All),
    properties = Map("prop_int32" -> containerPropertyInt,
      "prop_text" -> containerPropertyText,
      "prop_timeseries" -> containerTimeSeriesProperty,
      "connection" -> containerPropertyDirectRelation),
    constraints = None,
    indexes = None
  )

  private val containerPropertyListBool = ContainerPropertyDefinition(
    defaultValue = None,
    description = Some("Prop list bool"),
    name = Some("Prop list bool"),
    `type` = PrimitiveProperty(PrimitivePropType.Boolean, list = Some(true))
  )

  private val containerPropertyListFloat64 = ContainerPropertyDefinition(
    defaultValue = None,
    description = Some("Prop list float64"),
    name = Some("Prop list float64"),
    `type` = PrimitiveProperty(PrimitivePropType.Float64, list = Some(true))
  )

  private val containerList = ContainerCreateDefinition(
    space = spaceName,
    externalId = containerListExternalId,
    name = Some(containerNameList),
    description = Some("this is a container of list types"),
    usedFor = Some(Usage.All),
    properties = Map(
      "prop_list_bool" -> containerPropertyListBool,
      "prop_list_float64" -> containerPropertyListFloat64
    ),
    constraints = None,
    indexes = None
  )

  override def beforeAll(): Unit = {
    testClient.spacesv3
      .createItems(Seq(SpaceCreateDefinition(space = spaceName)))
      .unsafeRunSync()

    testClient.containers.createItems(Seq(containerPrimitive, containerList)).unsafeRunSync()
    ()
  }

  val viewVersion1 = "v1"


  it should "create a view" in {
    val containerReference = ContainerReference(spaceName, containerPrimitiveExternalId)
    val properties = Map(
      "prop_int32" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_int32"),
      "prop_text" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_text"),
      "prop_timeseries" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_timeseries")
    )
    val viewExternalId = "scala_sdk_view_1"

    val viewToCreate = ViewCreateDefinition(
      space = spaceName,
      externalId = viewExternalId,
      name = Some("first view"),
      description = Some("desc"),
      filter = None,
      implements = None,
      version = viewVersion1,
      properties = properties
    )

    val created = testClient.views
      .createItems(Seq(viewToCreate))
      .unsafeRunSync().headOption

    created.map(_.space) shouldBe Some(spaceName)
    created.map(_.externalId) shouldBe Some(viewExternalId)
    created.flatMap(_.name) shouldBe viewToCreate.name
    created.flatMap(_.description) shouldBe viewToCreate.description
    created.map(_.version) shouldBe Some(viewToCreate.version)

    created.map(_.properties) shouldBe Some(
      Map(
        "prop_int32" -> ViewCorePropertyDefinition(
          nullable = Some(true),
          autoIncrement = Some(false),
          defaultValue = Some(Int32(1)),
          `type` = PropertyType.PrimitiveProperty(`type` = PrimitivePropType.Int32),
          container = Some(containerReference),
          containerPropertyIdentifier = Some("prop_int32")
        ),
        "prop_text" -> ViewCorePropertyDefinition(
          nullable = Some(true),
          autoIncrement = Some(false),
          defaultValue = Some(PropertyDefaultValue.String("toto")),
          `type` = PropertyType.TextProperty(),
          container = Some(containerReference),
          containerPropertyIdentifier = Some("prop_text")
        ),
        "prop_timeseries" -> ViewCorePropertyDefinition(
          nullable = Some(true),
          autoIncrement = Some(false),
          defaultValue = Some(TimeSeriesReference("flux-capacitor-levels")),
          `type` = PropertyType.TimeSeriesReference(Some(false)),
          container = Some(containerReference),
          containerPropertyIdentifier = Some("prop_timeseries")
        )
      )
    )

  }

  it should "create a view that implement another view" in {
    val implementedViewExternalId = "scala_sdk_view_1"
    val implementingViewExternalId = "scala_sdk_view_2"

    val containerPrimReference = ContainerReference(spaceName, containerPrimitiveExternalId)
    val containerListReference = ContainerReference(spaceName, containerListExternalId)

    // Create a second view that reference to scala_sdk_test_view_1
    val implements =
      Seq(ViewReference(space = spaceName, externalId = implementedViewExternalId, version = "v1"))
    val properties2 = Map(
      "prop_int32" -> ViewPropertyCreateDefinition.CreateViewProperty(
        container = containerPrimReference,
        containerPropertyIdentifier = "prop_int32"
      ),
      "prop_list_float64" -> ViewPropertyCreateDefinition.CreateViewProperty(
        container = containerListReference,
        containerPropertyIdentifier = "prop_list_float64"
      )
    )
    val view2ToCreate = ViewCreateDefinition(
      space = spaceName,
      externalId = implementingViewExternalId,
      name = Some("second view"),
      description = Some("some desc"),
      implements = Some(implements),
      version = viewVersion1,
      properties = properties2
    )

    testClient.views
      .createItems(Seq(view2ToCreate))
      .unsafeRunSync()

    testClient.views
      .deleteItems(Seq(DataModelReference(spaceName, implementingViewExternalId, Some(viewVersion1))))
      .unsafeRunSync()
  }

  it should "create and retrieve a view that contains a RDR" in {

    val viewWithDRExternalId = "scala_sdk_view_3"
    val viewWithRDRExternalId = "scala_sdk_view_4"
    val containerPrimReference = ContainerReference(spaceName, containerPrimitiveExternalId)

    //connection property points to a container property that is a direct connection
    //reverse direct connections can only point to direct connections
    val viewWithDRProperties = Map(
      "connection" -> CreateViewProperty(
        None,
        None,
        containerPrimReference,
        "connection"
      )
    )
    val viewPointedTo = ViewCreateDefinition(
      space = spaceName,
      externalId = viewWithDRExternalId,
      name = Some("first view"),
      description = Some("desc"),
      filter = None,
      implements = None,
      version = viewVersion1,
      properties = viewWithDRProperties
    )

    val reverseDirectRelationProperty = CreateConnectionDefinition(
      ReverseDirectRelationConnection(
        MultiReverseDirectRelation,
        Some("name"),
        Some("desc"),
        ViewReference(spaceName, viewPointedTo.externalId, viewVersion1),
        ThroughReference("connection", ViewReference(spaceName, viewPointedTo.externalId, viewVersion1))
      )
    )

    val viewWithRDRProperties = Map(
      f"has_$viewWithDRExternalId" -> reverseDirectRelationProperty
    )

    val viewWithReverseDirectRelationship = ViewCreateDefinition(
      space = spaceName,
      externalId = viewWithRDRExternalId,
      name = Some("first view"),
      description = Some("desc"),
      filter = None,
      implements = None,
      version = viewVersion1,
      properties = viewWithRDRProperties
    )

    testClient.views
      .createItems(Seq(viewPointedTo, viewWithReverseDirectRelationship))
      .unsafeRunSync()

    val retrievedViews = testClient.views
      .retrieveItems(Seq(DataModelReference(spaceName, viewWithRDRExternalId, Some(viewVersion1))))
      .unsafeRunSync()
    retrievedViews.headOption.map(_.externalId) shouldBe Some(viewWithRDRExternalId)
    retrievedViews.headOption.flatMap(_.properties
      .get(f"has_$viewWithDRExternalId")
      .map(_.asInstanceOf[ReverseDirectRelationConnection].connectionType)) shouldBe Some(MultiReverseDirectRelation)
  }

  it should "delete views" in {
    val containerReference = ContainerReference(spaceName, containerPrimitiveExternalId)
    val viewExternalId = "scala_sdk_view_5"
    val properties = Map(
      "prop_int32" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_int32"),
      "prop_text" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_text"),
      "prop_timeseries" -> ViewPropertyCreateDefinition.CreateViewProperty(container = containerReference, containerPropertyIdentifier = "prop_timeseries")
    )
    val viewToCreate = ViewCreateDefinition(
      space = spaceName,
      externalId = viewExternalId,
      name = Some("first view"),
      description = Some("desc"),
      filter = None,
      implements = None,
      version = viewVersion1,
      properties = properties
    )
    testClient.views
      .createItems(Seq(viewToCreate))
      .unsafeRunSync().headOption

    testClient.views
      .deleteItems(Seq(DataModelReference(spaceName, viewExternalId, Some(viewVersion1))))
      .unsafeRunSync()
    val retrievedAfterDelete = testClient.views
      .retrieveItems(Seq(DataModelReference(spaceName, viewExternalId, Some(viewVersion1))))
      .unsafeRunSync()
    retrievedAfterDelete.size shouldBe 0

    //    // TODO This should produce CdpAPIException
    //    val sCaught = intercept[SdkException] {
    //      blueFieldClient.views.deleteItems(Seq(DataModelReference("test", "test", "test"))).unsafeRunSync()
    //    }
    //    sCaught.responseCode shouldBe  Some(404)
  }
}
