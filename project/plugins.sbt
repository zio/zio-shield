addSbtPlugin(
  "io.get-coursier" % "sbt-coursier" % "1.1.0-M6"
)

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
)
