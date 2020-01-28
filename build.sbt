import Dependencies._
import BuildHelper._

inThisBuild(
  Seq(
    organization := "dev.zio",
    homepage := Some(url("https://github.com/zio/zio-shield")),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/zio-shield/"), "scm:git:git@github.com:zio/zio-shield.git")
    ),
    developers := List(
      Developer(
        "vovapolu",
        "Vladimir Polushin",
        "vovapolu@gmail.com",
        url("https://github.com/vovapolu")
      )
    )
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = project
  .in(file("."))
  .settings(buildInfoSettings("zio.shield"))
  .enablePlugins(BuildInfoPlugin)
  .aggregate(shieldApi, shieldSbt, shieldTests, shieldDetector, docs)

lazy val shieldApi = project
  .in(file("shield-api"))
  .enablePlugins(GitVersioning)
  .settings(
    stdSettings("zio-shield")
  )
  .settings(
    name := "zio-shield-api",
    libraryDependencies ++= Seq(
      scalafixCore,
      scalafixRules,
      scaluzzi,
      circeCore,
      circeGeneric,
      circeGenericExtras,
      circeYaml,
      utest % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val shieldSbt = project
  .in(file("shield-sbt"))
  .dependsOn(shieldApi)
  .settings(
    name := "zio-shield",
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx2048M",
      s"-Dplugin.version=${version.value}"
    )
  )
  .enablePlugins(SbtPlugin, GitVersioning)

lazy val shieldTests = project
  .in(file("shield-tests"))
  .dependsOn(shieldApi) // for direct semantic document loading
  .settings(
    name := "zio-shield-tests",
    skip in publish := true,
    libraryDependencies ++= Seq(
      utest      % "test",
      zio        % "test",
      zioStreams % "test",
      compilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.2.3" cross CrossVersion.full)
    ),
    scalacOptions += "-Yrangepos",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val shieldDetector = project
  .in(file("shield-detector"))
  .settings(
    name := "zio-shield-detector",
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.reflections" % "reflections" % "0.9.11"
    )
  )

lazy val docs = project
  .in(file("docs"))
  .settings(
    skip in publish := true,
    moduleName := "zio-shield-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    target in (ScalaUnidoc, unidoc) := (baseDirectory in LocalRootProject).value / "website" / "static" / "api",
    cleanFiles += (target in (ScalaUnidoc, unidoc)).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(unidoc in Compile).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(unidoc in Compile).value
  )
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
