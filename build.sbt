import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.3.0-SNAPSHOT"
ThisBuild / organization := "com.vovapolu"

lazy val shieldApi = (project in file("shield-api"))
  .settings(
    name := "zio-shield-api",
    libraryDependencies ++= Seq(
      scalafixCore,
      scalafixRules,
      scalafixReflect,
      scaluzzi,
      utest % "test",
      compilerPlugin(
        "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full)
    ),
    scalacOptions += "-Yrangepos",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val shieldSbt = (project in file("shield-sbt"))
  .dependsOn(shieldApi)
  .enablePlugins(SbtPlugin)
  .settings(
    name := "zio-shield",
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )
