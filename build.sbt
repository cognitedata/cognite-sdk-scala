import wartremover.Wart
import sbt.project

val scala213 = "2.13.1"
val scala212 = "2.12.10"
val scala211 = "2.11.12"
val supportedScalaVersions = List(scala212, scala213, scala211)

// This is used only for tests.
val jettyTestVersion = "9.4.27.v20200227"

val sttpVersion = "1.6.8"
val circeVersion: Option[(Long, Long)] => String = {
  case Some((2, 13)) => "0.12.0-M4"
  case _ => "0.11.1"
}
val circeDerivationVersion: Option[(Long, Long)] => String = {
  case Some((2, 13)) => "0.12.0-M4"
  case _ => "0.11.0-M1"
}
val catsEffectVersion: Option[(Long, Long)] => String = {
  case Some((2, 13)) => "2.0.0-M4"
  case _ => "1.3.1"
}
val fs2Version: Option[(Long, Long)] => String = {
  case Some((2, 13)) => "1.1.0-M1"
  case _ => "1.0.5"
}

lazy val gpgPass = Option(System.getenv("GPG_KEY_PASSWORD"))

lazy val commonSettings = Seq(
  name := "cognite-sdk-scala",
  organization := "com.cognite",
  organizationName := "Cognite",
  organizationHomepage := Some(url("https://cognite.com")),
  version := "1.2.3-SNAPSHOT",
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
  wartremoverErrors in (Compile, compile) :=
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
  wartremoverExcluded += baseDirectory.value / "target" / "protobuf-generated"
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (target.value / "protobuf-generated")
)
managedSourceDirectories in Compile += target.value / "protobuf-generated"

lazy val core = (project in file("."))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.scalaland" %% "chimney" % "0.3.5",
      "commons-io" % "commons-io" % "2.6",
      "org.eclipse.jetty" % "jetty-server" % jettyTestVersion % Test,
      "org.eclipse.jetty" % "jetty-servlet" % jettyTestVersion % Test,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "co.fs2" %% "fs2-core" % fs2Version(CrossVersion.partialVersion(scalaVersion.value))
    ) ++ scalaTestDeps ++ sttpDeps ++ circeDeps(CrossVersion.partialVersion(scalaVersion.value))
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](organization, version, organizationName),
    buildInfoPackage := "BuildInfo"
  )

val scalaTestDeps = Seq(
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)
val sttpDeps = Seq(
  "com.softwaremill.sttp" %% "core" % sttpVersion,
  "com.softwaremill.sttp" %% "circe" % sttpVersion,
  "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpVersion % Test
)

def circeDeps(scalaVersion: Option[(Long, Long)]): Seq[ModuleID] =
  Seq(
    "io.circe" %% "circe-core" % circeVersion(scalaVersion),
    "io.circe" %% "circe-derivation" % circeDerivationVersion(scalaVersion),
    "io.circe" %% "circe-parser" % circeVersion(scalaVersion)
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

mainScalastyle := scalastyle.in(Compile).toTask("").value
testScalastyle := scalastyle.in(Test).toTask("").value

(test in Test) := (test in Test).dependsOn(testScalastyle).value
(test in Test) := (test in Test).dependsOn(mainScalastyle).value
