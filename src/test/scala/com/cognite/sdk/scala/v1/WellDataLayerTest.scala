package com.cognite.sdk.scala.v1

import cats.effect.IO
import cats.Monad
import com.cognite.sdk.scala.common.{CdpApiException, SdkTestSpec}
import io.circe.{Json, JsonObject, Printer}
import org.scalatest.{BeforeAndAfter, OptionValues}

@SuppressWarnings(
  Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.OptionPartial")
)
class WellDataLayerTest extends SdkTestSpec with BeforeAndAfter with OptionValues {
  implicit class JsonImplicits(actual: JsonObject) {
    def dropTimestamps(): JsonObject =
      actual.filterKeys(key => !Seq("createdTime", "lastUpdatedTime").contains(key))
    def shouldDeepEqual(expected: JsonObject): Unit = {
      val printer = Printer.spaces2.withSortedKeys.copy(dropNullValues = true)
      val expectedStr = Json.fromJsonObject(expected.dropTimestamps()).printWith(printer)
      val actualStr = Json.fromJsonObject(actual.dropTimestamps()).printWith(printer)
      val _ = expectedStr shouldEqual actualStr
    }
  }
  private val wdl = client.wdl

  before {
    val sources = wdl.sources.list().unsafeRunSync()
    wdl.sources.deleteRecursive(sources).unsafeRunSync()
  }

  it should "create, retrieve, and delete sources" in {
    val newSource = Source(shortRandom())
    wdl.sources.create(Seq(newSource)).unsafeRunSync().size shouldEqual 1

    val sources = wdl.sources.list().unsafeRunSync().map(source => source)
    sources should contain(newSource)

    wdl.sources.delete(Seq(newSource)).unsafeRunSync()
    wdl.sources.list().unsafeRunSync().map(source => source) should not contain newSource
  }

  it should "create and retrieve JsonObject source items" in {
    val source1 = JsonObject(
      ("name", Json.fromString("WITSML")),
      ("description", Json.fromString("The WITSML data source"))
    )

    val source2 = JsonObject(
      ("name", Json.fromString("EDM")),
      ("description", Json.fromString("Engineering Data Model"))
    )

    wdl.setItems("sources", Seq(source1, source2)).unsafeRunSync()

    val sources = wdl.listItemsWithGet("sources").unsafeRunSync()
    sources.items.size shouldEqual 2

    sources.items.headOption.value.shouldDeepEqual(
      JsonObject(
        "name" -> Json.fromString("WITSML"),
        "description" -> Json.fromString("The WITSML data source")
      )
    )

    sources
      .items(1)
      .shouldDeepEqual(
        JsonObject(
          "name" -> Json.fromString("EDM"),
          "description" -> Json.fromString("Engineering Data Model")
        )
      )
  }

  it should "create and retrieve JsonObject well items" in {
    val newSourceName = s"A-${shortRandom()}"
    wdl.sources.create(Seq(Source(newSourceName))).unsafeRunSync().size shouldEqual 1
    wdl.wells.setMergeRules(WellMergeRules(Seq(newSourceName))).unsafeRunSync()
    wdl.wellbores.setMergeRules(WellboreMergeRules(Seq(newSourceName))).unsafeRunSync()

    val well1 = JsonObject(
      ("matchingId", Json.fromString("deterministic-id")),
      ("name", Json.fromString("my new well")),
      (
        "source",
        Json.fromFields(
          Seq(
            ("sourceName", Json.fromString(newSourceName)),
            ("assetExternalId", Json.fromString("well1"))
          )
        )
      ),
      (
        "wellhead",
        Json.fromFields(
          Seq(
            ("x", Json.fromDoubleOrNull(42.42)),
            ("y", Json.fromDoubleOrNull(12.34)),
            ("crs", Json.fromString("EPSG:4326"))
          )
        )
      )
    )

    val expected = JsonObject(
      ("matchingId", Json.fromString("deterministic-id")),
      ("name", Json.fromString("my new well")),
      (
        "sources",
        Json.arr(
          Json.fromFields(
            Seq(
              ("sourceName", Json.fromString(newSourceName)),
              ("assetExternalId", Json.fromString("well1"))
            )
          )
        )
      ),
      ("wellbores", Json.arr()),
      (
        "wellhead",
        Json.fromFields(
          Seq(
            ("x", Json.fromDoubleOrNull(42.42)),
            ("y", Json.fromDoubleOrNull(12.34)),
            ("crs", Json.fromString("EPSG:4326"))
          )
        )
      )
    )

    wdl.setItems("wells", Seq(well1)).unsafeRunSync()
    val results = wdl.listItemsWithPost("wells/list").unsafeRunSync()
    results.items.size shouldEqual 1

    results.items.headOption.value.shouldDeepEqual(expected)
  }

  it should "transform json with the transformBody hook" in {
    // first, create a new source
    val newSource = Source(name = shortRandom())
    wdl.sources.create(Seq(newSource)).unsafeRunSync().size shouldEqual 1

    // Then set merge rules
    wdl.wells.setMergeRules(WellMergeRules(Seq(newSource.name))).unsafeRunSync()
    wdl.wellbores.setMergeRules(WellboreMergeRules(Seq(newSource.name))).unsafeRunSync()
    wdl.wells.create(
      Seq(
        WellSource(
          name = "well-1",
          source = AssetSource("well-1", newSource.name),
          field = Some("VOLVE"),
          wellhead = Some(Wellhead(0, 0, "EPSG:4326"))
        ),
        WellSource(
          name = "well-2",
          source = AssetSource("well-2", newSource.name),
          field = Some("Utsira"),
          wellhead = Some(Wellhead(0, 0, "EPSG:4326"))
        )
      )
    ).unsafeRunSync()

    val items = wdl.listItemsWithPost(
      "wells/list",
      transformBody = jsonObject =>
        jsonObject
          .add(
            "filter",
            Json.fromFields(
              Seq(
                (
                  "field",
                  Json.fromFields(
                    Seq(
                      ("oneOf", Json.fromValues(Seq(Json.fromString("VOLVE"))))
                    )
                  )
                )
              )
            )
          )
    ).unsafeRunSync()
    items.items.size should be(1)
    val well = items.items.headOption.get
    well.toMap("name").asString.get should be("well-1")
  }

  it should "create, retrieve, and delete wells" in {
    // first, create a new source
    val newSource = Source(name = shortRandom())
    wdl.sources.create(Seq(newSource)).unsafeRunSync().size shouldEqual 1

    // Then set merge rules
    wdl.wells.setMergeRules(WellMergeRules(Seq(newSource.name))).unsafeRunSync()
    wdl.wellbores.setMergeRules(WellboreMergeRules(Seq(newSource.name))).unsafeRunSync()

    wdl.wells
      .create(
        Seq(
          WellSource(
            name = "my new well",
            source = AssetSource("A:my new well", newSource.name),
            wellhead = Some(Wellhead(0.0, 60.0, "EPSG:4326"))
          )
        )
      )
      .unsafeRunSync()
      .size shouldEqual 1

    val wellboreSource = WellboreSource(
      name = "my new wellbore",
      source = AssetSource("A:my new wellbore", newSource.name),
      datum = Some(Datum(54.9, "meter", "KB")),
      wellAssetExternalId = "A:my new well"
    )

    wdl.wellbores.create(Seq(wellboreSource)).unsafeRunSync().size shouldEqual 1

    val retrievedWellboreSource = wdl.wellboreSources.list().unsafeRunSync().headOption.value
    retrievedWellboreSource.name should be("my new wellbore")
    retrievedWellboreSource.source should be(wellboreSource.source)
    retrievedWellboreSource.datum should be(wellboreSource.datum)
    retrievedWellboreSource.wellAssetExternalId should be(wellboreSource.wellAssetExternalId)

    val wells = wdl.wells.list().unsafeRunSync()
    wells.size shouldEqual 1
    val well = wells.headOption.value
    well.name shouldEqual "my new well"
    well.sources.headOption.value should equal(AssetSource("A:my new well", newSource.name))
    well.wellhead shouldEqual Wellhead(0.0, 60.0, "EPSG:4326")

    well.wellbores match {
      case None => sys.error("No wellbores")
      case Some(wellbores) =>
        wellbores.size shouldEqual 1
        val wellbore = wellbores.headOption.value
        wellbore.name shouldEqual "my new wellbore"
        wellbore.sources shouldEqual Seq(AssetSource("A:my new wellbore", newSource.name))
        wellbore.datum shouldEqual Some(Datum(54.9, "meter", "KB"))
    }

    wdl.sources.deleteRecursive(Seq(newSource)).unsafeRunSync()
    val sourcesAfterDeletion = wdl.sources.list().unsafeRunSync()
    sourcesAfterDeletion.size shouldEqual 0
  }

  it should "get schema for Source" in {
    val schema = wdl.getSchema("Source").unsafeRunSync()
    schema.length should be >= 0
  }

  it should "fail when getting schema for Wololo" in {
    val thrown = intercept[CdpApiException] {
      wdl.getSchema("Wololo").unsafeRunSync()
    }
    thrown.message shouldEqual "Unknown schema: Wololo"
  }

  it should "be safe to create 0 wells" in {
    wdl.wells.create(Seq()).unsafeRunSync() shouldBe Seq()
  }

  it should "be safe to delete 0 well sources" in {
    wdl.wellSources.delete(Seq()) shouldBe Monad[IO].unit
  }

  it should "be safe to create 0 sources" in {
    wdl.sources.create(Seq()).unsafeRunSync() shouldBe Seq()
  }

  it should "be safe to delete 0 sources" in {
    wdl.sources.delete(Seq()) shouldBe Monad[IO].unit
  }

  it should "be safe to create 0 wellbores" in {
    wdl.wellbores.create(Seq()).unsafeRunSync() shouldBe Seq()
  }
}
