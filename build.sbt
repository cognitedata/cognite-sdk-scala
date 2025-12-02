import sbt.Keys.javacOptions
import sbt.{Test, project}
import wartremover.Wart

val scala3 = "3.3.3"
val scala213 = "2.13.16"
val supportedScalaVersions = List(scala213, scala3)

val javaVersion = "11"

// This is used only for tests.
val jettyTestVersion = "11.0.25"

val sttpVersion = "3.11.0"
val circeVersion = "0.14.15"
val catsEffectVersion = "3.6.3"
val fs2Version = "3.12.2"
val natchezVersion = "0.3.7"

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.5.0"

lazy val patchVersion = scala.io.Source.fromFile("patch_version.txt").mkString.trim

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "central.sonatype.com",
  System.getenv("SONATYPE_USERNAME"),
  System.getenv("SONATYPE_PASSWORD")
)
credentials += Credentials("Artifactory Realm",
  "cognite.jfrog.io",
  System.getenv("JFROG_USERNAME"),
  System.getenv("JFROG_PASSWORD"),
)

val artifactory = "https://cognite.jfrog.io/cognite"

javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "2.34." + patchVersion,
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
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else localStaging.value
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
      "com.google.protobuf" % "protobuf-java" % "4.33.0",
      "org.tpolecat" %% "natchez-core" % natchezVersion,
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value)),
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        List(
          "-Wconf:cat=deprecation:i",
          "-Wconf:msg=discarded non-Unit value of type org.scalatest.Assertion:s",
          "-Wconf:msg=discarded non-Unit value of type org.scalatest.compatible.Assertion:s",

          "-source:3.0-migration",
        )
      case Some((2, minor)) if minor == 13 =>
        List(
          "-Wconf:cat=deprecation:i",
          "-Wconf:cat=other-pure-statement:i",
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
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-parser_2.13"),
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion % Test
)

def circeDeps(scalaVersion: Option[(Long, Long)]): Seq[ModuleID] =
  Seq(
    // We use the cats version included in the cats-effect version we specify.
    ("io.circe" %% "circe-core" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-generic" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-parser" % circeVersion)
      .exclude("org.typelevel", "cats-core_2.13"),
    ("io.circe" %% "circe-literal" % circeVersion % Test)
      .exclude("org.typelevel", "cats-core_2.13")
  )

scalacOptions --= (CrossVersion.partialVersion(scalaVersion.value) match {
  case _ =>
    List.empty[String]
})

Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
