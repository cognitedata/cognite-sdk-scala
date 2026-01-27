addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.4")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2-1")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.4")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.2")
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.233")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.4.1")
addSbtPlugin("software.purpledragon" % "sbt-dependency-lock" % "1.5.1")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"
