name := "cognite-sdk-scala"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"
val circeVersion = "0.11.1"
val sttpVersion = "1.5.0"
val jsoniterVersion = "0.46.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  organization := "com.cognite",
)

lazy val core = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.scalaland" %% "chimney" % "0.3.1",
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps
  )

val scalaTestDeps = Seq(
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
)
val sttpDeps = Seq(
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion
)

val circeDeps = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

addCompilerPlugin(scalafixSemanticdb)
scalacOptions ++= List(
  "-Yrangepos",          // required by SemanticDB compiler plugin
  "-Ywarn-unused-import" // required by `RemoveUnused` rule
)

wartremoverErrors ++= Warts.allBut(
  Wart.DefaultArguments,
  Wart.Nothing,
  Wart.Any,
  Wart.Throw,
  Wart.ImplicitParameter,
  Wart.ToString)

scalastyleFailOnWarning := true

lazy val mainScalastyle = taskKey[Unit]("mainScalastyle")
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

mainScalastyle := scalastyle.in(Compile).toTask("").value
testScalastyle := scalastyle.in(Test).toTask("").value

(test in Test) := ((test in Test) dependsOn testScalastyle).value
(test in Test) := ((test in Test) dependsOn mainScalastyle).value
