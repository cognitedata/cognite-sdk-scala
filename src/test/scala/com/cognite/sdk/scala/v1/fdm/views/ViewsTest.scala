// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.v1.fdm.views

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.fdm.common.PropertyDefinition.{ContainerPropertyDefinition, ViewPropertyDefinition}
import com.cognite.sdk.scala.v1.fdm.common.PropertyType.PrimitiveProperty
import com.cognite.sdk.scala.v1.fdm.common.{PropertyDefaultValue, PropertyType}
import com.cognite.sdk.scala.v1.fdm.containers._
import com.cognite.sdk.scala.v1.{CommonDataModelTestHelper, SpaceCreateDefinition}
//import com.cognite.sdk.scala.common.{DSLAndFilter, DSLEqualsFilter, DSLInFilter, RetryWhile}
import com.cognite.sdk.scala.common.RetryWhile
//import com.cognite.sdk.scala.v1.resources.Containers.containerCreateEncoder
//import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterAll

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product"
  )
)
class ViewsTest extends CommonDataModelTestHelper with RetryWhile with BeforeAndAfterAll {
  private val spaceName = "test-space-scala-sdk"
  private val containerNamePrim = "scala sdk container prim"
  private val containerPrimitiveExternalId = "scala_sdk_container_primitive"

  private val containerNameList = "scala sdk container list"
  private val containerListExternalId = "scala_sdk_container_list"

  private val containerPropertyInt = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.Double(1.0)),
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

  private val containerPrimitive = ContainerCreate(
    space = spaceName,
    externalId = containerPrimitiveExternalId,
    name = Some(containerNamePrim),
    description = Some("this is a container of primitive types"),
    usedFor = Some(ContainerUsage.All),
    properties = Map("prop_int32" -> containerPropertyInt, "prop_text" -> containerPropertyText),
    constraints = None,
    indexes = None
  )

  private val containerPropertyListBool = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.Boolean(true)),
    description = Some("Prop list bool"),
    name = Some("Prop list bool"),
    `type` = PrimitiveProperty(PrimitivePropType.Boolean, list = Some(true))
  )

  private val containerPropertyListFloat64 = ContainerPropertyDefinition(
    defaultValue = Some(PropertyDefaultValue.Double(1.0)),
    description = Some("Prop list float64"),
    name = Some("Prop list float64"),
    `type` = PrimitiveProperty(PrimitivePropType.Float64, list = Some(true))
  )

  private val containerList = ContainerCreate(
    space = spaceName,
    externalId = containerListExternalId,
    name = Some(containerNameList),
    description = Some("this is a container of list types"),
    usedFor = Some(ContainerUsage.All),
    properties = Map(
      "prop_list_bool" -> containerPropertyListBool,
      "prop_list_float64" -> containerPropertyListFloat64
    ),
    constraints = None,
    indexes = None
  )

  override def beforeAll(): Unit = {
    blueFieldClient.spacesv3
      .createItems(Seq(SpaceCreateDefinition(space = spaceName)))
      .unsafeRunSync()

    val createdContainer =
      blueFieldClient.containers.createItems(Seq(containerPrimitive, containerList)).unsafeRunSync()
    println(s"createdContainer = ${createdContainer.toString()}")
    //    println(s"createdContainer = ${containerPrimitive.toString()}, ${containerList.toString()}")
  }

  val viewVersion1 = "v1"
  val viewExternalId = "scala_sdk_view_1"
  val view2ExternalId = "scala_sdk_view_2"
  val view3ExternalId = "scala_sdk_view_3"

  it should "create a view" in {
    val containerReference = ContainerReference(spaceName, containerPrimitiveExternalId)
    val properties = Map(
      "prop_int32" -> CreatePropertyReference(containerReference, "prop_int32"),
      "prop_text" -> CreatePropertyReference(containerReference, "prop_text")
    )
    val viewToCreate = ViewCreateDefinition(
      space = spaceName,
      externalId = viewExternalId,
      name = Some("first view"),
      description = Some("desc"),
      //         filter = Some(DSLAndFilter(Seq(
      //           DSLInFilter(property = Seq("dummy", "dummy2"), `values` = Seq(
      //             PropertyType.Bigint.Property(9223372036854775L),
      //             PropertyType.Text.Property("abcdef"),
      //           )),
      //           DSLEqualsFilter(property = Seq("dummy"),  `value` = PropertyType.Text.Property("testValue"))))),
      // implements = Some(implements),
      version = Some(viewVersion1),
      properties = properties
    )

    val created = blueFieldClient.views
      .createItems(Seq(viewToCreate))
      .unsafeRunSync()

    created.headOption.map(_.space) shouldBe Some(spaceName)
    created.headOption.map(_.externalId) shouldBe Some(viewExternalId)
    created.headOption.flatMap(_.name) shouldBe viewToCreate.name
    created.headOption.flatMap(_.description) shouldBe viewToCreate.description
    // created.headOption.flatMap(_.implements) shouldBe None // Some(implements)
    created.headOption.flatMap(_.version) shouldBe viewToCreate.version

    created.headOption.map(_.properties) shouldBe Some(
      Map(
        "prop_int32" -> ViewPropertyDefinition(
          nullable = Some(true),
          autoIncrement = Some(false),
          defaultValue = None,
          `type` = PropertyType.PrimitiveProperty(`type` = PrimitivePropType.Int32),
          container = Some(containerReference),
          containerPropertyIdentifier = None
        ),
        "prop_text" -> ViewPropertyDefinition(
          nullable = Some(true),
          autoIncrement = Some(false),
          defaultValue = None,
          `type` = PropertyType.TextProperty(),
          container = Some(containerReference),
          containerPropertyIdentifier = None
        )
      )
    )

  }

  it should "create a view that implement another view" in {
    val containerPrimReference = ContainerReference(spaceName, containerPrimitiveExternalId)
    val containerListReference = ContainerReference(spaceName, containerListExternalId)

    // Create a second view that reference to scala_sdk_test_view_1
    val implements =
      Seq(ViewReference(space = spaceName, externalId = "viewExternalId", version = "v1"))
    val properties2 = Map(
      "prop_int32" -> CreatePropertyReference(containerPrimReference, "prop_int32"),
      "prop_list_float64" -> CreatePropertyReference(containerListReference, "prop_list_float64")
    )

    val view2ToCreate = ViewCreateDefinition(
      space = spaceName,
      externalId = view2ExternalId,
      name = Some("second view"),
      description = Some("some desc"),
      implements = Some(implements),
      version = Some(viewVersion1),
      properties = properties2
    )

    val created2 = blueFieldClient.views
      .createItems(Seq(view2ToCreate))
      .unsafeRunSync()
    println(s"created2 = ${created2.toString()}")
  }

  ignore should "retrieve views by data model reference" in {
    val view1 = blueFieldClient.views
      .retrieveItems(Seq(DataModelReference(spaceName, viewExternalId, "v1")))
      .unsafeRunSync()
      .headOption
    view1.map(_.space) shouldBe Some("test1")
    view1.map(_.externalId) shouldBe Some(viewExternalId)
    view1.flatMap(_.name) shouldBe Some("first view")
    view1.flatMap(_.description) shouldBe Some("desc")
    view1.flatMap(_.version) shouldBe Some(viewVersion1)

    val view2 = blueFieldClient.views
      .retrieveItems(Seq(DataModelReference(spaceName, view2ExternalId, "v1")))
      .unsafeRunSync()
      .headOption
    view2.map(_.space) shouldBe Some(spaceName)
    view2.map(_.externalId) shouldBe Some(view2ExternalId)
    view2.flatMap(_.name) shouldBe Some("second view")
    view2.flatMap(_.description) shouldBe Some("desc")
    view2.flatMap(_.version) shouldBe Some(viewVersion1)
  }

  ignore should "delete views" in {
    blueFieldClient.views
      .deleteItems(Seq(DataModelReference(spaceName, viewExternalId, viewVersion1)))
      .unsafeRunSync()

    val retrievedAfterDelete = blueFieldClient.views
      .retrieveItems(Seq(DataModelReference(spaceName, viewExternalId, viewVersion1)))
      .unsafeRunSync()
    retrievedAfterDelete.size shouldBe 0

    //    // TODO This should produce CdpAPIException
    //    val sCaught = intercept[SdkException] {
    //      blueFieldClient.views.deleteItems(Seq(DataModelReference("test", "test", "test"))).unsafeRunSync()
    //    }
    //    sCaught.responseCode shouldBe  Some(404)
  }
}