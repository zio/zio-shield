package zio.shield.sbt

import java.io.FileNotFoundException
import java.nio.file.Paths

import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}
import zio.shield.config.{Config => ZioShieldConfig}
import zio.shield.semdocs.DirectSemanticDocumentLoader
import zio.shield.{ZioShield, ZioShieldDiagnostic}

import scala.util.Try

object ZioShieldPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin

  object autoImport {
    val shield: TaskKey[Unit] =
      taskKey[Unit]("Run ZIO Shield")
    val shieldFatalWarnings: SettingKey[Boolean] =
      settingKey[Boolean](
        "Make all lint and patch warnings fatal, e.g. throwing error instead of warning."
      )
    val shieldConfigPath: SettingKey[Option[String]] =
      settingKey[Option[String]](
        "Relative path to ZIO Shield config. By default is \".shield\" in the project root."
      )

    def shieldConfigSettings(config: Configuration): Seq[Def.Setting[_]] =
      Seq(
        shield := (shieldTask(config) dependsOn (compile in config)).value
      )
  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    shieldConfigPath := None,
    shieldFatalWarnings := false
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      libraryDependencies ++= Seq(compilerPlugin(
        "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full)),
      scalacOptions += "-Yrangepos"
    ) ++
      Seq(Compile, Test).flatMap(c => inConfig(c)(shieldConfigSettings(c)))

  private def shieldTask(
      config: Configuration
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log

      val pathStr = shieldConfigPath.value.getOrElse(".shield.yaml")

      log.info(s"""Reading ZIO Shield config from "$pathStr"""")

      val configE = for {
        path <- Try { baseDirectory.value.toPath.resolve(Paths.get(pathStr)) }.toEither
        config <- ZioShieldConfig.fromFile(path)
      } yield config

      val zioShieldConfig = configE match {
        case Left(_: FileNotFoundException)
            if (shieldConfigPath.value.isEmpty) =>
          ZioShieldConfig.empty
        case Left(err) =>
          log.error(s"Error while loading config: $err")
          ZioShieldConfig.empty
        case Right(c) =>
          c
      }

      zioShieldConfig.excludedRules.foreach { er =>
        if (ZioShield.allSemanticRules.exists(_.name.value == er)) {
          log.warn(
            s""""$er" is not a supported rule, no rule will be excluded""")
        }
      }

      // TODO add check for inferrers

      val zioShield = ZioShield(
        DirectSemanticDocumentLoader(fullClasspath.value.map(_.data.toPath).toList)
      ).withConfig(zioShieldConfig)

      val files = unmanagedSources.in(config).value.map(_.toPath).toList

      var isError = false

      log.info("Building ZIO Shield cache...")

      val onDiagnostic: ZioShieldDiagnostic => Unit = d =>
        if (shieldFatalWarnings.value) {
          isError = true
          log.error(d.consoleMessage)
        } else {
          log.warn(d.consoleMessage)
      }

      zioShield.updateCache(files)(onDiagnostic)

      val stats = zioShield.cacheStats

      log.info(f"""||ZIO Shield Statistics|
                   ||---------------------|
                   ||Files: ${stats.filesCount}%14s|
                   ||Symbols: ${stats.symbolsCount}%12s|
                   ||Edges: ${stats.edgesCount}%14s|""".stripMargin)

      log.info("Running ZIO Shield...")

      zioShield.run(files)(onDiagnostic)

      if (isError) {
        throw new ZioShieldFailed()
      }
    }
}
