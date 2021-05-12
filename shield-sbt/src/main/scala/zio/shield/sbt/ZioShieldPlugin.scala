package zio.shield.sbt

import java.io.FileNotFoundException

import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{ Def, _ }
import zio.shield.config.{ Config => ZioShieldConfig }
import zio.shield.semdocs.DirectSemanticDocumentLoader
import zio.shield.{ ZioShield, ZioShieldDiagnostic }

object ZioShieldPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = JvmPlugin

  object autoImport {
    val shield: TaskKey[Unit] =
      taskKey[Unit]("Run ZIO Shield")
    val shieldFatalWarnings: SettingKey[Boolean] =
      settingKey[Boolean](
        "Make all lint and patch warnings fatal, e.g. throwing error instead of warning."
      )
    val shieldConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        "Optional location of ZIO Shield config. " +
          "If not specified config is read from \".shield.yaml\" in the project root."
      )
    val shieldDebugOutput: SettingKey[Boolean] =
      settingKey[Boolean](
        "Turn on verbose output for debugging purposes."
      )

    def shieldConfigSettings(config: Configuration): Seq[Def.Setting[_]] =
      Seq(
        shield := (shieldTask(config) dependsOn (compile in config)).value
      )
  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    shieldFatalWarnings := false,
    shieldDebugOutput := false
  )

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    shieldConfig := None
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      libraryDependencies ++= {
        if (!libraryDependencies.value.exists(_.name == "semanticdb-scalac"))
          Seq(compilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.2.3" cross CrossVersion.full))
        else
          Seq.empty
      },
      scalacOptions ++= {
        if (!libraryDependencies.value.exists(_.name == "semanticdb-scalac"))
          Seq(
            "-Yrangepos",
            "-Xplugin-require:semanticdb"
          )
        else
          Seq.empty
      }
    ) ++
      Seq(Compile, Test).flatMap(c => inConfig(c)(shieldConfigSettings(c)))

  private def shieldTask(
    config: Configuration
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log

      val path = shieldConfig
        .in(config)
        .value
        .getOrElse((baseDirectory in ThisBuild).value / ".shield.yaml")
        .toPath
      log.info(s"""Reading ZIO Shield config from "${path.toAbsolutePath}"""")

      val configE = ZioShieldConfig.fromFile(path)
      val zioShieldConfig = configE match {
        case Left(_: FileNotFoundException) if (shieldConfig.in(config).value.isEmpty) =>
          ZioShieldConfig.empty
        case Left(err) =>
          log.error(s"Error while loading config: $err")
          ZioShieldConfig.empty
        case Right(c) =>
          c
      }

      ZioShield.validateConfig(zioShieldConfig).foreach { t =>
        log.warn(t.toString)
        log.warn("Ignoring invalid rules and inferrers.")
      }

      val zioShield = ZioShield(
        DirectSemanticDocumentLoader(fullClasspath.value.map(_.data.toPath).toList)
      ).withConfig(zioShieldConfig)

      val files = unmanagedSources.value.map(_.toPath).toList

      var isError = false

      log.info("Building ZIO Shield cache...")

      val onDiagnostic: ZioShieldDiagnostic => Unit = d =>
        if (shieldFatalWarnings.in(config).value) {
          isError = true
          log.error(d.consoleMessage)
        } else {
          log.warn(d.consoleMessage)
        }

      zioShield.updateCache(files)(onDiagnostic)

      val stats = zioShield.cacheStats

      if (shieldDebugOutput.value) {
        log.info(f"""||ZIO Shield Statistics|
                     ||---------------------|
                     ||Files: ${stats.filesCount}%14s|
                     ||Symbols: ${stats.symbolsCount}%12s|
                     ||Edges: ${stats.edgesCount}%14s|""".stripMargin)

        log.info(s"Leaf symbols:\n${stats.leafSymbols.mkString("  ", "\n  ", "\n")}")
      }

      log.info("Running ZIO Shield...")

      zioShield.run(files)(onDiagnostic)

      if (isError) {
        throw new ZioShieldFailed()
      }
    }
}
