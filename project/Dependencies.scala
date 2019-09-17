import sbt._

object Dependencies {
  lazy val scala212 = "2.12.10"
  lazy val scala213 = "2.13.0"

  lazy val circeVersion = "0.11.1"

  lazy val scalafixCore = "ch.epfl.scala" %% "scalafix-core" % "0.9.7"
  lazy val scalafixRules = "ch.epfl.scala" %% "scalafix-rules" % "0.9.7"
  lazy val scalafixReflect = "ch.epfl.scala" % "scalafix-reflect" % "0.9.7" cross CrossVersion.full
  lazy val scaluzzi = "com.github.vovapolu" %% "scaluzzi" % "0.1.3"
  lazy val utest = "com.lihaoyi" %% "utest" % "0.7.1"
  lazy val zio = "dev.zio" %% "zio" % "1.0.0-RC8-6"
  lazy val circeYaml = "io.circe" %% "circe-yaml" % "0.10.0"
  lazy val circeCore =  "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric =  "io.circe" %% "circe-generic" % circeVersion
  lazy val circeGenericExtras =  "io.circe" %% "circe-generic-extras" % circeVersion
}
