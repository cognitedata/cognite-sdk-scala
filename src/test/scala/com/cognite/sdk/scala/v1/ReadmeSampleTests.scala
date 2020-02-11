package com.cognite.sdk.scala.v1

import java.time._
import java.util.concurrent.Executors

import scala.concurrent._
import cats.effect.IO
import com.cognite.sdk.scala.common.{ApiKeyAuth, Auth, DataPoint}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.scalatest._

object ReadmeSampleTests {
  implicit val cs = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
  def auth(): Auth =
    Option(System.getenv("TEST_API_KEY_READ"))
      .map(ApiKeyAuth(_, None))
      .getOrElse({
        throw new Exception("TEST_API_KEY_READ env variable is not set")
      })

  private def prepareClient(): GenericClient[IO, Nothing] = {
    implicit val sttpBackend = AsyncHttpClientCatsBackend[cats.effect.IO]()
    new GenericClient[IO, Nothing]("scala-sdk-examples", projectName="playground", auth())
  }
}
// If something has to be fixed here, it should be also probably fixed in README.md
class ReadmeSampleTests extends FlatSpec {
  "TimeSeries read examples should" should "work" in {
    val c = ReadmeSampleTests.prepareClient()

    val timeSeries: List[TimeSeries] = c.timeSeries.list(Some(12)).compile.toList.unsafeRunSync()
    assert(12 === timeSeries.length)

    val timeSeriesId = timeSeries.head.id;
    val singleTimeSerie: TimeSeries = c.timeSeries.retrieveById(timeSeriesId).unsafeRunSync()
    assert(singleTimeSerie.id === timeSeriesId)

    val dataPoints = c.dataPoints.queryById(
      timeSeriesId,
      inclusiveStart = Instant.ofEpochMilli(0),
      exclusiveEnd = Instant.now()).unsafeRunSync()
    assert(dataPoints.externalId.isDefined)
    assert(dataPoints.externalId === singleTimeSerie.externalId)

    val latestPoints: Map[Long, Option[DataPoint]] = c.dataPoints.getLatestDataPointsByIds(Seq(timeSeriesId)).unsafeRunSync()

    assert(latestPoints.keys.toList === List(timeSeriesId))

    val aggregates = Seq("count", "average", "max")
    val aggregatedSeries = c.dataPoints
                            .queryAggregatesById(timeSeriesId, Instant.ofEpochMilli(0L), Instant.now(), granularity="1d", aggregates)
                            .unsafeRunSync()
    val firstDay = aggregatedSeries("average").flatMap(_.datapoints).map(_.timestamp).min
    val expectedFirstDay = dataPoints.datapoints.map(_.timestamp).min
    assert(firstDay.minus(Duration.ofDays(1)).compareTo(expectedFirstDay) < 0)
    assert(firstDay.plus(Duration.ofDays(1)).compareTo(expectedFirstDay) > 0)
  }
}
