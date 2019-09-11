import Dependencies._

inThisBuild(
  Seq(
    scalaVersion := "2.12.8",
    organization := "zio.shield",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
)

lazy val shieldApi = (project in file("shield-api"))
  .enablePlugins(GitVersioning)
  .settings(
    moduleName := "zio-shield-api",
    libraryDependencies ++= Seq(
      scalafixCore,
      scalafixRules,
      scalafixReflect,
      scaluzzi,
      circeCore,
      circeGeneric,
      circeYaml
    )
  )

lazy val shieldSbt = (project in file("shield-sbt"))
  .enablePlugins(GitVersioning, SbtPlugin)
  .dependsOn(shieldApi)
  .settings(
    moduleName := "zio-shield",
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )

lazy val shieldTests = (project in file("shield-tests"))
  .dependsOn(shieldApi) // for direct semantic document loading
  .settings(
    moduleName := "zio-tests",
    libraryDependencies ++= Seq(
      utest % "test",
      zio % "test",
      compilerPlugin(
        "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full)
    ),
    scalacOptions += "-Yrangepos",
    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
