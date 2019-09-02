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

val c = Client("scala-sdk-examples")
```

Create a client using the cats-effect `IO`:

```scala
import com.cognite.sdk.scala.v1._
import java.util.concurrent.Executors
import scala.concurrent._
import cats.effect.IO
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend

implicit val cs = IO.contextShift(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
implicit val sttpBackend = AsyncHttpClientCatsBackend[cats.effect.IO]()
val c = Client("scala-sdk-examples")
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
c.dataPoints.queryById(timeSeriesId, 0, System.currentTimeMillis())
```

Note that aggregates are not yet supported.

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
