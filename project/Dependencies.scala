import sbt._

object Dependencies {
  lazy val zioVersion         = "1.0.0-RC17"
  lazy val circeVersion       = "0.12.3"
  lazy val circeExtrasVersion = "0.12.2"

  lazy val scalafixCore       = "ch.epfl.scala"       %% "scalafix-core"        % "0.9.11"
  lazy val scalafixRules      = "ch.epfl.scala"       %% "scalafix-rules"       % "0.9.11"
  lazy val scaluzzi           = "com.github.vovapolu" %% "scaluzzi"             % "0.1.3"
  lazy val utest              = "com.lihaoyi"         %% "utest"                % "0.7.3"
  lazy val zio                = "dev.zio"             %% "zio"                  % zioVersion
  lazy val zioStreams         = "dev.zio"             %% "zio-streams"          % zioVersion
  lazy val circeYaml          = "io.circe"            %% "circe-yaml"           % "0.12.0"
  lazy val circeCore          = "io.circe"            %% "circe-core"           % circeVersion
  lazy val circeGeneric       = "io.circe"            %% "circe-generic"        % circeVersion
  lazy val circeGenericExtras = "io.circe"            %% "circe-generic-extras" % circeExtrasVersion
}
