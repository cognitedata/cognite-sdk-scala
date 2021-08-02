import wartremover.Wart
import sbt.project

val scala3 = "3.0.0"
val scala213 = "2.13.6"
val scala212 = "2.12.14"
val scala211 = "2.11.12"
val supportedScalaVersions = List(scala212, scala213, scala211, scala3)

// This is used only for tests.
val jettyTestVersion = "11.0.6"

val sttpVersion = "3.3.7"
val circeVersion: Option[(Long, Long)] => String = {
  case Some((2, 11)) => "0.12.0-M3"
  case _ => "0.14.1"
}
val catsEffectVersion: Option[(Long, Long)] => String = {
  case Some((2, 11)) => "2.0.0"
  case _ => "2.5.1"
}
val fs2Version: Option[(Long, Long)] => String = {
  case Some((2, 11)) => "2.1.0"
  case _ => "2.5.6"
}

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "1.5.1",
  crossScalaVersions := supportedScalaVersions,
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
      // Scala 2.11 is no longer supported by sbt-scoverage
      case Some((2, 11)) => false
      // Scala 3 is not supported by scoverage
      // https://github.com/scoverage/scalac-scoverage-plugin/issues/299
      case Some((3, _)) => false
      case _ => coverageEnabled.value
    }
  },
  Compile / wartremoverErrors :=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        // Scala 2.11 is no longer supported by Wartremover
        Seq.empty[wartremover.Wart]
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
      "org.typelevel" %% "cats-effect" % catsEffectVersion(
        CrossVersion.partialVersion(scalaVersion.value)
      ),
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion(
        CrossVersion.partialVersion(scalaVersion.value)
      ) % Test,
      "co.fs2" %% "fs2-core" % fs2Version(CrossVersion.partialVersion(scalaVersion.value)),
      "com.google.protobuf" % "protobuf-java" % "3.15.8"
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value)),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor == 13 =>
        List(
          // We use JavaConverters to remain backwards compatible with Scala 2.11
          "-Wconf:cat=deprecation:i"
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
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
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
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats-ce2" % sttpVersion % Test
)

def circeDeps(scalaVersion: Option[(Long, Long)]): Seq[ModuleID] =
  Seq(
    // We use the cats version included in the cats-effect version we specify.
    ("io.circe" %% "circe-core" % circeVersion(scalaVersion))
      .exclude("org.typelevel", "cats-core_2.11")
      .exclude("org.typelevel", "cats-core_2.12")
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-generic" % circeVersion(scalaVersion))
      .exclude("org.typelevel", "cats-core_2.11")
      .exclude("org.typelevel", "cats-core_2.12")
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-parser" % circeVersion(scalaVersion))
      .exclude("org.typelevel", "cats-core_2.11")
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

// Scala 2.11 doesn't support mixed projects as ours, so just disable docs for that release.
Compile / doc / sources := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, minor)) if minor == 11 =>
    Seq.empty
  case _ =>
    (Compile / doc / sources).value
})
