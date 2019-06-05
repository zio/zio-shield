import Dependencies._

ThisBuild / scalaVersion := "2.12.8"

lazy val shieldApi = (project in file("shield-api"))
  .enablePlugins(SbtPlugin)
  .settings(
    version := "0.2.0",
    organization := "com.vovapolu",
    name := "zio-shield",
    libraryDependencies ++= Seq(
      scalafixCore,
      scalafixRules,
      scalafixReflect,
      scaluzzi,
      coursierSmall,
      compilerPlugin(
        "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full)),
    scalacOptions ++= List(
      "-Ywarn-unused",
      "-Yrangepos",
      "-target:jvm-1.8"
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )
