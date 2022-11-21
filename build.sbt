import wartremover.Wart
import sbt.project

val scala3 = "3.2.0"
val scala213 = "2.13.8"
val scala212 = "2.12.17"
val supportedScalaVersions = List(scala212, scala213, scala3)

// This is used only for tests.
val jettyTestVersion = "9.4.48.v20220622"

val sttpVersion = "3.5.2"
val circeVersion = "0.14.2"
val catsEffectVersion = "3.3.14"
val fs2Version = "3.3.0"

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.1.5"

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "2.4.6",
  crossScalaVersions := supportedScalaVersions,
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  description := "Scala SDK for Cognite Data Fusion.",
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/cognitedata/cognite-sdk-scala")),
  developers := List(
    Developer(
      id = "wjoel",
      name = "Joel Wilsson",
      email = "joel.wilsson@cognite.com",
      url = url("https://wjoel.com")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/cognitedata/cognite-sdk-scala"),
      "scm:git@github.com:cognitedata/cognite-sdk-scala.git"
    )
  ),
  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  publishMavenStyle := true,
  pgpPassphrase := {
    if (gpgPass.isDefined) gpgPass.map(_.toCharArray)
    else None
  },
  coverageEnabled := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // Scala 3 is not supported by scoverage
      // https://github.com/scoverage/scalac-scoverage-plugin/issues/299
      case Some((3, _)) => false
      case _ => coverageEnabled.value
    }
  },
  Compile / wartremoverErrors :=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        // We do this to make IntelliJ happy, as it doesn't understand Wartremover properly.
        // Since we currently put 2.12 first in cross version, that's what IntelliJ is using.
        Seq.empty[wartremover.Wart]
      case _ =>
        Warts.allBut(
          Wart.DefaultArguments,
          Wart.Nothing,
          Wart.Any,
          Wart.Throw,
          Wart.ImplicitParameter,
          Wart.ToString,
          Wart.Overloading
        )
    })
)

lazy val core = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoUsePackageAsPath := true,
    buildInfoKeys := Seq[BuildInfoKey](organization, version, organizationName),
    buildInfoPackage := "BuildInfo"
  )
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.11.0",
      "org.eclipse.jetty" % "jetty-server" % jettyTestVersion % Test,
      "org.eclipse.jetty" % "jetty-servlet" % jettyTestVersion % Test,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
      "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion % Test,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "com.google.protobuf" % "protobuf-java" % "3.21.4"
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value)),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor == 13 =>
        List(
          // We use JavaConverters to remain backwards compatible with Scala 2.12,
          // and to avoid a dependency on scala-collection-compat
          "-Wconf:cat=deprecation:i"
        )
      case Some((2, minor)) if minor == 12 =>
        List(
          // Scala 2.12 doesn't always handle @nowarn correctly,
          // and doesn't seem to like @deprecated case class fields with default values.
          "-Wconf:src=src/main/scala/com/cognite/sdk/scala/v1/resources/assets.scala&cat=deprecation:i"
        )
      case Some((3, _)) => List("-source:3.0-migration")
      case _ =>
        List.empty[String]
    }),
    compileOrder := CompileOrder.JavaThenScala,
    PB.additionalDependencies := Seq.empty,
    // Fixes collision with BuildInfo, see
    // https://github.com/thesamet/sbt-protoc/issues/6#issuecomment-353028192
    PB.deleteTargetDirectory := false,
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value
    )
  )

val scalaTestDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.14" % "test"
)
val sttpDeps = Seq(
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  ("com.softwaremill.sttp.client3" %% "circe" % sttpVersion)
    // We specify our own version of circe.
    .exclude("io.circe", "circe-core_2.11")
    .exclude("io.circe", "circe-core_2.12")
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-parser_2.11")
    .exclude("io.circe", "circe-parser_2.12")
    .exclude("io.circe", "circe-parser_2.13"),
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion % Test
)

def circeDeps(scalaVersion: Option[(Long, Long)]): Seq[ModuleID] =
  Seq(
    // We use the cats version included in the cats-effect version we specify.
    ("io.circe" %% "circe-core" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.12")
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-generic" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.12")
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-parser" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.12")
      .exclude("org.typelevel", "cats-core_2.13")
  )

scalacOptions --= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, minor)) if minor == 12 =>
    // disable those in 2.12 due to invalid warnings
    List(
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:params"
    )
  case _ =>
    List.empty[String]
})

scalastyleFailOnWarning := true

lazy val mainScalastyle = taskKey[Unit]("mainScalastyle")
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

mainScalastyle := (Compile / scalastyle).toTask("").value
testScalastyle := (Test / scalastyle).toTask("").value

Test / test := (Test / test).dependsOn(testScalastyle).value
Test / test := (Test / test).dependsOn(mainScalastyle).value
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

// Scala 2.11 doesn't support mixed projects as ours, so just disable docs for that release.
Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, minor)) if minor == 11 =>
    Seq.empty
  case _ =>
    (Compile / doc / sources).value
})
