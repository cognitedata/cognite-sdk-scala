//// Copyright 2020 Cognite AS
//// SPDX-License-Identifier: Apache-2.0
//
//package com.cognite.sdk.scala.v1
//
//import cats.effect.unsafe.implicits.global
//import com.cognite.sdk.scala.common.{DSLAndFilter, DSLEqualsFilter, DSLInFilter, RetryWhile}
//import com.cognite.sdk.scala.v1.containers.ContainerPropertyType.PrimitiveProperty
//import com.cognite.sdk.scala.v1.containers._
//import com.cognite.sdk.scala.v1.resources.Containers.containerCreateEncoder
//import io.circe.syntax.EncoderOps
//import org.scalatest.BeforeAndAfterAll
//
//import java.util.UUID
//
//@SuppressWarnings(
//  Array(
//    "org.wartremover.warts.PublicInference",
//    "org.wartremover.warts.NonUnitStatements"
//  )
//)
//class ViewsTest2 extends CommonDataModelTestHelper with RetryWhile with BeforeAndAfterAll {
//  private val spaceName = "test-space-scala-sdk"
//  private val containerName = "test-container-scala-sdk"
//
//  override def beforeAll(): Unit = {
//    val existingSpaces = blueFieldClient.spacesv3.retrieveItems(Seq(SpaceById(spaceName))).unsafeRunSync()
//
//    val spaceResponse = blueFieldClient.spacesv3.createItems(Seq(
//      SpaceCreateDefinition(space = spaceName, description = Some("Test space for scala-sdk tests"), name = Some(spaceName)))
//    ).unsafeRunSync()
//
//    println(existingSpaces)
//    println(spaceResponse)
//    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place
//
//    val containerProperty = ContainerPropertyDefinition(
//      defaultValue = Some(PropertyDefaultValue.Double(1.0)),
//      description = Some("Test numeric property"),
//      name = Some("numeric-property-prop-1"),
//      `type` = PrimitiveProperty(`type` = PrimitivePropType.Int32)
//    )
//
//    val containerToCreate = ContainerCreate(
//      space = spaceName,
//      externalId = uuid,
//      name = Some(containerName),
//      description = Some("this is a test space for scala sdk"),
//      usedFor = Some(ContainerUsage.All),
//      properties = Map("property-1" -> containerProperty),
//      constraints = None,
//      indexes = None
//    )
//
//    println(s" = ${containerToCreate.asJson}")
//
//    val response = blueFieldClient.containers.createItems(containers = Seq(containerToCreate)).unsafeRunSync()
//    println(s"response = ${response}")
//    ()
//  }
//
//  it should "aha" in {
//    assert(true)
//  }
//
//  ignore should "create views" in {
//    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place
//    val implements = Seq(ViewReference(space = "space", externalId = "viewExternalId", version = "1.0.1"))
//    val containerReference = ContainerReference("space", "containerExternalId")
//    val properties = Map("prop1" -> CreatePropertyReference(container = containerReference,
//      externalId = "cont1", name = Some("cont1"), description = Some("hello"))
//    )
//    val created = blueFieldClient.views.createItems(Seq(
//      ViewCreateDefinition(
//         space = "test",
//         externalId = uuid,
//         name = Some("test"),
//         description = Some("desc"),
//         filter = Some(DSLAndFilter(Seq(
//           DSLInFilter(property = Seq("dummy", "dummy2"), `values` = Seq(
//             PropertyType.Bigint.Property(9223372036854775L),
//             PropertyType.Text.Property("abcdef"),
//           )),
//           DSLEqualsFilter(property = Seq("dummy"), `value` = PropertyType.Text.Property("testValue"))))),
//         implements = Some(implements),
//         version = Some("5.0.0"),
//         properties = properties
//    ))).unsafeRunSync()
//    created.head.space shouldBe "test"
//    created.head.externalId shouldBe uuid
//    created.head.name shouldBe Some("test")
//    created.head.description shouldBe Some("desc")
//    created.head.implements shouldBe Some(implements)
//    created.head.version shouldBe Some("5.0.0")
//
//    created.head.properties shouldBe  Map("prop1" -> ViewPropertyDefinition(externalId = "cont1",
//      nullable = None,
//      autoIncrement = None,
//      description = Some("hello"),
//      `type` = None,
//      container = Some(containerReference),
//      containerPropertyExternalId = None)
//    )
//  }
//
//
//  ignore should "retrieve views by data model reference" in {
//    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place
//
//    blueFieldClient.views.createItems(Seq(
//      ViewCreateDefinition(
//        space = "test1",
//        externalId = uuid,
//        name = Some("test1"),
//        description = Some("desc"),
//        filter = None,
//        implements = None,
//        version = Some("6.0.0"),
//        properties = Map()
//      ))).unsafeRunSync()
//
//    val retrieved = blueFieldClient.views.retrieveItems(Seq(DataModelReference("test1", uuid, "6.0.0"))).unsafeRunSync().head
//    retrieved.space shouldBe "test1"
//    retrieved.externalId shouldBe uuid
//    retrieved.name shouldBe Some("test1")
//    retrieved.description shouldBe Some("desc")
//    retrieved.version shouldBe Some("6.0.0")
//  }
//
//  ignore should "delete views" in {
//    val uuid = UUID.randomUUID.toString // TODO no need to use uuid for externalId when API is in place
//
//    blueFieldClient.views.createItems(Seq(
//      ViewCreateDefinition(
//        space = "test",
//        externalId = uuid,
//        name = Some("test"),
//        description = Some("desc"),
//        filter = None,
//        implements = None,
//        version = Some("5.0.0"),
//        properties = Map()
//      ))).unsafeRunSync()
//    val retrieved = blueFieldClient.views.retrieveItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
//    retrieved.head.externalId shouldBe uuid
//
//    blueFieldClient.views.deleteItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
//    val retrievedAfterDelete = blueFieldClient.views.retrieveItems(Seq(DataModelReference("test", uuid, "5.0.0"))).unsafeRunSync()
//    retrievedAfterDelete.size shouldBe(0)
//
////    // TODO This should produce CdpAPIException
////    val sCaught = intercept[SdkException] {
////      blueFieldClient.views.deleteItems(Seq(DataModelReference("test", "test", "test"))).unsafeRunSync()
////    }
////    sCaught.responseCode shouldBe  Some(404)
//  }
//}
//
//object ViewsTest2 {
//
//}
