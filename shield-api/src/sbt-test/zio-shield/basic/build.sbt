ThisBuild / scalaVersion := "2.12.8"

lazy val example = project
  .settings(
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full),
    scalacOptions ++= List(
      "-Yrangepos",
      "-Ywarn-unused-import"
    )
//    libraryDependencies ++= Seq(
//      "ch.epfl.scala" %% "scalafix-rules" % "0.9.5",
//      "com.github.vovapolu" %% "scaluzzi" % "0.1.2"
//    )
  )
