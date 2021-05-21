import wartremover.Wart
import sbt.project

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val scala213 = "2.13.6"
val scala212 = "2.12.13"
val scala211 = "2.11.12"
val supportedScalaVersions = List(scala212, scala213, scala211)

// This is used only for tests.
val jettyTestVersion = "9.4.41.v20210516"

val sttpVersion = "3.3.3"
val circeVersion: Option[(Long, Long)] => String = {
  case Some((2, 11)) => "0.12.0-M3"
  case _ => "0.14.0-M7"
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
  version := "1.4.7-SNAPSHOT",
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
    // Scala 2.11 is no longer supported by sbt-scoverage
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => false
      case _ => coverageEnabled.value
    }
  },
  Compile / wartremoverErrors :=
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor <= 11 =>
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
    }),
)

lazy val protoBufs = project.in(file("protobufs"))
  .settings(
    commonSettings,
    name := "cognite-sdk-scala-protobufs",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    scalacOptions -= "-Xfatal-warnings",
    Test / wartremoverErrors := Seq.empty[wartremover.Wart],
    Compile / wartremoverErrors := Seq.empty[wartremover.Wart],
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value,
    ),
  )

lazy val core = (project in file("."))
  .aggregate(protoBufs)
  .dependsOn(protoBufs)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.scalaland" %% "chimney" % "0.5.3",
      "commons-io" % "commons-io" % "2.8.0",
      "org.eclipse.jetty" % "jetty-server" % jettyTestVersion % Test,
      "org.eclipse.jetty" % "jetty-servlet" % jettyTestVersion % Test,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.typelevel" %% "cats-effect" % catsEffectVersion(CrossVersion.partialVersion(scalaVersion.value)),
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion(CrossVersion.partialVersion(scalaVersion.value)) % Test,
      "co.fs2" %% "fs2-core" % fs2Version(CrossVersion.partialVersion(scalaVersion.value))
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value))
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoUsePackageAsPath := true,
    buildInfoKeys := Seq[BuildInfoKey](organization, version, organizationName),
    buildInfoPackage := "BuildInfo",
)

val scalaTestDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)
val sttpDeps = Seq(
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % sttpVersion
    // We specify our own version of circe.
    exclude("io.circe", "circe-core_2.11")
    exclude("io.circe", "circe-core_2.12")
    exclude("io.circe", "circe-core_2.13")
    exclude("io.circe", "circe-parser_2.11")
    exclude("io.circe", "circe-parser_2.12")
    exclude("io.circe", "circe-parser_2.13"),
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats-ce2" % sttpVersion % Test
)

def circeDeps(scalaVersion: Option[(Long, Long)]): Seq[ModuleID] =
  Seq(
    // We use the cats version included in the cats-effect version we specify.
    "io.circe" %% "circe-core" % circeVersion(scalaVersion)
      exclude("org.typelevel", "cats-core_2.11")
      exclude("org.typelevel", "cats-core_2.12")
      exclude("org.typelevel", "cats-core_2.13"),
    "io.circe" %% "circe-generic" % circeVersion(scalaVersion)
      exclude("org.typelevel", "cats-core_2.11")
      exclude("org.typelevel", "cats-core_2.12")
      exclude("org.typelevel", "cats-core_2.13"),
    "io.circe" %% "circe-parser" % circeVersion(scalaVersion)
      exclude("org.typelevel", "cats-core_2.11")
      exclude("org.typelevel", "cats-core_2.12")
      exclude("org.typelevel", "cats-core_2.13")
  )

//addCompilerPlugin(scalafixSemanticdb)
scalacOptions ++= List(
  "-Yrangepos" // required by SemanticDB compiler plugin
  //"-Ywarn-unused-import" // required by `RemoveUnused` rule
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

mainScalastyle := (Compile/scalastyle).toTask("").value
testScalastyle := (Test/scalastyle).toTask("").value

Test/test := (Test/test).dependsOn(testScalastyle).value
Test/test := (Test/test).dependsOn(mainScalastyle).value
