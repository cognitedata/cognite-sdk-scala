import sbt.{Test, project}
import wartremover.Wart

val scala3 = "3.3.3"
val scala213 = "2.13.16"
val scala212 = "2.12.19"
val supportedScalaVersions = List(scala212, scala213, scala3)

// This is used only for tests.
val jettyTestVersion = "9.4.56.v20240826"

val sttpVersion = "3.5.2"
val circeVersion = "0.14.10"
val catsEffectVersion = "3.5.7"
val fs2Version = "3.11.0"
val natchezVersion = "0.3.7"

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.4.0"

lazy val patchVersion = scala.io.Source.fromFile("patch_version.txt").mkString.trim

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  System.getenv("SONATYPE_USERNAME"),
  System.getenv("SONATYPE_PASSWORD")
)
credentials += Credentials("Artifactory Realm",
  "cognite.jfrog.io",
  System.getenv("JFROG_USERNAME"),
  System.getenv("JFROG_PASSWORD"),
)

val artifactory = "https://cognite.jfrog.io/cognite"

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "2.31." + patchVersion,
  isSnapshot := patchVersion.endsWith("-SNAPSHOT"),
  scalaVersion := scala213, // use 2.13 by default
  // handle cross plugin https://github.com/stringbean/sbt-dependency-lock/issues/13
  dependencyLockFile := baseDirectory.value /
    s"build.scala-${CrossVersion
        .partialVersion(scalaVersion.value) match {
        case Some((2, n)) => s"2.$n"
        case Some((3, _)) => s"3"
      }}.sbt.lock",
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
  publishTo := (if (System.getenv("PUBLISH_TO_JFROG") == "true") {
    if (isSnapshot.value)
      Some("snapshots".at(s"$artifactory/libs-snapshot-local/"))
    else
      Some("local-releases".at(s"$artifactory/libs-release-local/"))
  } else {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  }),
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
          Wart.Overloading,
          Wart.SeqApply,
          Wart.SeqUpdated
        )
    })
)

lazy val core = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoUsePackageAsPath := true,
    buildInfoKeys := Seq[BuildInfoKey](organization, version, organizationName),
    buildInfoPackage := "com.cognite.scala_sdk"
  )
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.18.0",
      "org.eclipse.jetty" % "jetty-server" % jettyTestVersion % Test,
      "org.eclipse.jetty" % "jetty-servlet" % jettyTestVersion % Test,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
      "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion % Test,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "com.google.protobuf" % "protobuf-java" % "4.29.3",
      "org.tpolecat" %% "natchez-core" % natchezVersion,
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value)),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        List(
          "-Wconf:cat=deprecation:i",
          // We use JavaConverters to remain backwards compatible with Scala 2.12,
          // and to avoid a dependency on scala-collection-compat
          "-Wconf:msg=object JavaConverters in package scala.collection is deprecated.*:s",
          "-Wconf:msg=method mapValues in trait MapOps is deprecated.*:s",
          "-Wconf:msg=discarded non-Unit value of type org.scalatest.Assertion:s",
          "-Wconf:msg=discarded non-Unit value of type org.scalatest.compatible.Assertion:s",

          "-source:3.0-migration",
        )
      case Some((2, minor)) if minor == 13 =>
        List(
          "-Wconf:cat=deprecation:i",
          "-Wconf:cat=other-pure-statement:i",
          // We use JavaConverters to remain backwards compatible with Scala 2.12,
          // and to avoid a dependency on scala-collection-compat
          "-Wconf:origin=scala.collection.compat.*:s"
        )
      case Some((2, minor)) if minor == 12 =>
        List(
          // Scala 2.12 doesn't always handle @nowarn correctly,
          // and doesn't seem to like @deprecated case class fields with default values.
          "-Wconf:src=src/main/scala/com/cognite/sdk/scala/v1/resources/assets.scala&cat=deprecation:i"
        )
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
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
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
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-literal" % circeVersion % Test)
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

Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
