package com.cognite.sdk.scala.v1.fdm.datamodels

import cats.effect.unsafe.implicits.global
import com.cognite.sdk.scala.v1.CommonDataModelTestHelper
import com.cognite.sdk.scala.v1.fdm.Utils
import com.cognite.sdk.scala.v1.fdm.common.{DataModelReference, Usage}
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyDefinition.ContainerPropertyDefinition
import com.cognite.sdk.scala.v1.fdm.common.properties.PropertyType
import com.cognite.sdk.scala.v1.fdm.containers.ContainerCreateDefinition
import com.cognite.sdk.scala.v1.fdm.views.{ViewCreateDefinition, ViewPropertyCreateDefinition}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.PublicInference",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.OptionPartial"
  )
)
@deprecated("message", since = "0")
class DataModelsTest extends CommonDataModelTestHelper {
  private val space = Utils.SpaceExternalId: @annotation.nowarn

  private val propsMap = Map(
    "stringProp1" -> ContainerPropertyDefinition(
      nullable = Some(true),
      autoIncrement = Some(false),
      defaultValue = None,
      description = Some("Test TextProperty NonList WithoutDefaultValue Nullable Description"),
      name = Some("Test-TextProperty-NonList-WithoutDefaultValue-Nullable-Name"),
      `type` = PropertyType.TextProperty(Some(false), Some("ucs_basic"))
    )
  )
  private val container = blueFieldClient.containers
    .createItems(containers =
        Seq(
          ContainerCreateDefinition(
          space = space,
          externalId = "testDataModelV3Container",
          name = Some(s"Test-Container-Scala-Sdk"),
          description = Some(s"Test Container For Scala SDK"),
          usedFor = Some(Usage.All),
          properties = propsMap,
          constraints = None,
          indexes = None
        )
      )
    ).unsafeRunSync().headOption: @annotation.nowarn

  private val view = blueFieldClient.views
    .createItems(items =
      Seq(
        ViewCreateDefinition(
          space = space,
          externalId = "testDataModelV3View",
          version = "v1",
          name = Some(s"Test-View-Scala-SDK"),
          description = Some("Test View For Scala SDK"),
          filter = None,
          properties = container.map {c =>
            c.properties.map {
              case (pName, _) =>
                pName -> ViewPropertyCreateDefinition.CreateViewProperty(
                  name = Some(pName),
                  container = c.toSourceReference,
                  containerPropertyIdentifier = pName)
            }
          }.getOrElse(Map.empty),
          implements = None
        )
      )
    ).unsafeRunSync().headOption: @annotation.nowarn


  "Datamodels" should "create models" in {
      val dataModel = blueFieldClient.dataModelsV3.createItems(items =
          Seq(
            DataModelCreate(
              space = space,
              externalId = "testDataModelV3",
              name = Some("testDataModelV3"),
              description = Some("testDataModelV3"),
              version = "v1",
              views = view.map(v => Vector(v.toSourceReference))
            )
        )
      ).unsafeRunSync().headOption


    dataModel.map(_.externalId) shouldBe Some("testDataModelV3")
    dataModel.flatMap(_.views.flatMap(_.headOption)) shouldBe view.map(_.toSourceReference)
  }

  "Datamodels" should "retrieve models" in {
    val dataModel = blueFieldClient.dataModelsV3.retrieveItems(items =
      Seq(
        DataModelReference(
          space = space,
          externalId = "testDataModelV3",
          version = Some("v1")
        )
      )
    ).unsafeRunSync().headOption


    dataModel.map(_.externalId) shouldBe Some("testDataModelV3")
    dataModel.flatMap(_.views.flatMap(_.headOption)) shouldBe view.map(_.toSourceReference)
  }
}
