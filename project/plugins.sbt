//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.18")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
// scala-steward:off
// This is the last version that still supports Scala 2.11
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
// scala-steward:on
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.227")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.13")


// Warning: These must be synced with
// https://github.com/cognitedata/cdp-spark-datasource/blob/master/project/protoc.sbt
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.31") // See warning above
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.7" // See warning above
