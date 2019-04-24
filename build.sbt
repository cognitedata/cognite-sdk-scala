name := "cognite-sdk-scala"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.7"
val circeVersion = "0.11.1"
val sttpVersion = "1.5.0"
val jsoniterVersion = "0.46.0"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "io.scalaland" %% "chimney" % "0.3.1"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)
