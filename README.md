# Cognite Scala SDK

*Under development, not recommended for production use cases*

This is the Cognite Scala SDK for developers working with
[Cognite Data Fusion](https://cognite.com/cognite/cognite-data-fusion/developers/).

Visit [Maven Repository](https://mvnrepository.com/artifact/com.cognite/cognite-sdk-scala)
to see the available versions, and how to include it as a dependency for sbt, Maven, Gradle,
and other build tools.

Authentication is specified using an implicit `Auth` parameter, which by default
looks for an API key to use in the `COGNITE_API_KEY` environment variable.

The `GenericClient` requires a backend for [sttp](https://github.com/softwaremill/sttp),
which can use any effects wrapper (for example,
[cats-effect](https://github.com/typelevel/cats-effect), but users who do not
want to use an effect wrapper can use `Client` to easily create a client using
an identity effect.

## Examples

Create a simple client, specifying an application name:

```scala
import com.cognite.sdk.scala.v1._

val c = Client("scala-sdk-examples", "https://api.cognitedata.com")
```

Create a client using the cats-effect `IO`:

```scala
import com.cognite.sdk.scala.v1._
import com.cognite.sdk.scala.common._
import java.util.concurrent.Executors
import scala.concurrent._
import cats.effect.IO
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend

implicit val cs = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
implicit val sttpBackend = AsyncHttpClientCatsBackend[cats.effect.IO]()
val auth = ApiKeyAuth(your API key comes here) // you can get the key at https://openindustrialdata.com/
val c = new GenericClient[IO, Nothing]("scala-sdk-examples", projectName="publicdata", auth)
```

The following examples will use `Client` with the identity effect.
Add `.unsafeRunSync` or similar to the end of the examples if you use `IO` or
another effect wrapper.

### Getting started with Ammonite

You can use [Ammonite](https://ammonite.io/) for a better interactive Scala environment,
and load the SDK directly like this:

```scala
import $ivy.`com.cognite::cognite-sdk-scala:0.1.2`
import com.cognite.sdk.scala.v1._
```

### Discover time series

List and filter endpoints use Stream from [fs2](https://github.com/functional-streams-for-scala/fs2),
which loads more data as required. You can use `.compile.toList` to convert it to a list,
but note that this could end up fetching a lot of data unless you limit it, for example by using
`.take(25)` as we do here.

For the next examples, you will need to supply ids for the time series that you want to retrieve.
You can find some ids by listing the available time series:

```scala
val timeSeriesList = c.timeSeries.list().take(25).compile.toList
val timeSeriesId = timeSeriesList.head.id
print(c.timeSeries.retrieveById(timeSeriesId))
```

### Retrieve data points

If you have a time series ID you can retrieve its data points:

```scala
val dataPoints = c.dataPoints.queryById(
      timeSeriesId,
      inclusiveStart=Instant.ofEpochMilli(0),
      exclusiveEnd=Instant.now())
```

It is also possible to query aggregate values using a time series ID. Possible aggregate values can be found in the API [documentation](https://docs.cognite.com/api/v1/#operation/getMultiTimeSeriesDatapoints)
You must also specify a granularity. 

```scala
val aggregates = Seq("count", "average", "max")
c.dataPoints.queryAggregatesById(timeSeriesId, Instant.ofEpochMilli(0L), Instant.now(), granularity="1d", aggregates)
```

If you need only the last data point for a time series or group of timeseries, you can retrieve these using:

```scala
val latestPoints: Map[Long, Option[DataPoint]] = c.dataPoints.getLatestDataPointsByIds(Seq(timeSeriesId))
```

This returns a map from each of the time series IDs specified in the function call to the latest data point for that
 time series, if it exists, or None if it does not.

There are analogous functions available to execute these queries using external IDs instead of IDs and returning string
valued data points rather than numeric valued data points.

### Insert and Delete Data

It is possible to insert and delete numeric data points for specified time series. To insert data, you must
specify the time series for which to insert data and the data points to insert:

```scala
val testDataPoints = (startTime to endTime by 1000).map(t => DataPoint(Instant.ofEpochMilli(t), math.random))
c.dataPoints.insertById(timeSeriesId, testDataPoints)
```

To delete data, you must specify the time series and the range of times for which to delete data:

```scala
c.dataPoints.deleteRangeById(timeSeriesId, Instant.ofEpochMilli(0L), Instant.now())
```

### Create an asset hierarchy

```scala
val root = AssetCreate(name = "root", externalId = Some("1"))
val child = AssetCreate(name = "child", externalId = Some("2"), parentExternalId = Some("1"))
val descendant = AssetCreate(name = "descendant", externalId = Some("3"), parentExternalId = Some("2"))
val createdAssets = c.assets.create(Seq(root, child, descendant))

// clean up the assets we created
c.assets.deleteByExternalIds(Seq("1", "2", "3"))
```

## Print an asset subtree using filter

```scala
def children(parents: Seq[Asset]): List[Asset] =
  c.assets.filter(AssetsFilter(parentIds = Some(parents.map(_.id)))).compile.toList

def printSubTree(parents: Seq[Asset], prefix: String = ""): Unit = {
  if (parents.nonEmpty) {
    val assetsUnderThisLevel = children(parents)
    parents.foreach { p =>
      println(prefix + p.name)
      val assetsUnderThisAsset = assetsUnderThisLevel.filter(_.parentId.exists(pid => pid == p.id))
      printSubTree(assetsUnderThisAsset, prefix + "  ")
    }
  }
}

val rootId = 2780934754068396L
printSubTree(c.assets.retrieveByIds(Seq(rootId)))
```

## Find all events in an asset subtree

```scala
def eventsForAssets(parents: Seq[Asset]): List[Event] =
  c.events.filter(EventsFilter(assetIds = Some(parents.map(_.id)))).compile.toList

def eventsInSubTree(parents: Seq[Asset]): Seq[Event] = {
  if (parents.nonEmpty) {
    val assetsUnderThisLevel = children(parents)
    eventsForAssets(parents) ++ parents.flatMap { p =>
      val assetsUnderThisAsset = assetsUnderThisLevel.filter(_.parentId.exists(pid => pid == p.id))
      eventsInSubTree(assetsUnderThisAsset)
    }
  } else {
    Seq.empty[Event]
  }
}

eventsInSubTree(c.assets.retrieveByIds(Seq(rootId))).foreach(event => println(event.`type`))
```
## 3D

To list 3D models:

```scala
val models = c.threeDModels.list().take(10).compile.toList
```

You can create, update, delete, and retrieve models based on their ID. To access model revisions for a specific model:

```scala
val modelId = modelIds.head.id
val revisions = c.threeDRevisions(modelId).list().take(10).compile.toList
```

You can also create, update, delete, and retrieve revisions based on their IDs. You can also list all the nodes in a
particular revision, as well as the ancestors of a particular node (including the node itself):

```scala
val revisionId = revisionIds.head.id
val nodes = c.threeDNodes(modelId, revisionId).list().listWithLimit(10).compile.toList
val nodeId = nodeIds.head.id
val ancestorNodes = c.threeDNodes(modelId, revisionId).ancestors(nodeId).compile.toList
```

