ThisBuild / scalaVersion := "2.12.8"

lazy val example = project
  .settings(
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full),
    scalacOptions ++= List(
      "-Yrangepos",
      "-Ywarn-unused-import"
    ),
    shieldFatalWarnings := true
  )
