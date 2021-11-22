import wartremover.Wart
import sbt.project

val scala3 = "3.0.1"
val scala213 = "2.13.7"
val scala212 = "2.12.15"
val supportedScalaVersions = List(scala212, scala213, scala3)

// This is used only for tests.
val jettyTestVersion = "9.4.44.v20210927"

val sttpVersion = "3.3.15"
val catsEffectVersion = "2.5.4"
val jsoniterScalaVersion = "2.12.0"

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "1.5.18",
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
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterScalaVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterScalaVersion % "compile",
      "org.eclipse.jetty" % "jetty-server" % jettyTestVersion % Test,
      "org.eclipse.jetty" % "jetty-servlet" % jettyTestVersion % Test,
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
      "com.google.protobuf" % "protobuf-java" % "3.19.1"
    ) ++ scalaTestDeps ++ sttpDeps,
    scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor == 13 =>
        List(
          // We use JavaConverters to avoid a dependency on scala-collection-compat,
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
  "org.scalatest" %% "scalatest" % "3.2.10" % "test"
)
val sttpDeps = Seq(
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "json-common" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats-ce2" % sttpVersion % Test
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
