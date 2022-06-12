//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.3.3")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.233")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.0.4")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"
