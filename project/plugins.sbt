addSbtPlugin(
  "io.get-coursier" % "sbt-coursier" % coursier.util.Properties.version
)

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
)
