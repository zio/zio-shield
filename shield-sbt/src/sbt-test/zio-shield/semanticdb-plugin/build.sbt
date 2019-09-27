ThisBuild / scalaVersion := "2.12.8"

lazy val example = project.settings(
  libraryDependencies += compilerPlugin(
    "org.scalameta" % "semanticdb-scalac" % "4.2.3" cross CrossVersion.full)
)
