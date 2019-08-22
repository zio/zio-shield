package zio.shield.sbt

import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}
import zio.shield.{ZioShield, ZioShieldDiagnostic}

object ZioShieldPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin

  object autoImport {
    val shield: TaskKey[Unit] =
      taskKey[Unit]("Run ZIO Shield")
    val shieldFatalWarnings: SettingKey[Boolean] =
      settingKey[Boolean](
        "Make all lint and patch warnings fatal, e.g. throwing error instead of warning"
      )
    val excludedRules: SettingKey[List[String]] =
      settingKey[List[String]](
        "Exclude specific rules from code analysis"
      )
    val excludedInferrers: SettingKey[List[String]] =
      settingKey[List[String]](
        "Exclude specific tag inferrers from code analysis. It can cause excluding dependent rules."
      )

    def shieldConfigSettings(config: Configuration): Seq[Def.Setting[_]] =
      Seq(
        shield := (shieldTask(config) dependsOn (compile in config)).value
      )
  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    shieldFatalWarnings := false,
    excludedRules := List.empty,
    excludedInferrers := List.empty
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

      val zioShield =
        ZioShield(scalacOptions.in(config).value.toList,
                  fullClasspath.value.map(_.data.toPath).toList).withAllRules()

      excludedRules.value.foreach { er =>
        if (!zioShield.syntacticRules.rules.exists(_.name.value == er) &&
            !zioShield.semanticRules.rules.exists(_.name.value == er)) {
          log.warn(
            s""""$er" is not a supported rule, no rule will be excluded""")
        }
      }

      excludedInferrers.value.foreach { ei =>
        if (!zioShield.inferrers.exists(_.name == ei)) {
          log.warn(s""""$ei" is not a supported inferrer""")
        }
      }

      val excludedZioShield =
        zioShield.exclude(excludedRules.value, excludedInferrers.value)

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

      excludedZioShield.updateCache(files)(onDiagnostic)

      val stats = excludedZioShield.cacheStats

      log.info(f"""||ZIO Shield Statistics|
                   ||---------------------|
                   ||Files: ${stats.filesCount}%14s|
                   ||Symbols: ${stats.symbolsCount}%12s|
                   ||Edges: ${stats.edgesCount}%14s|""".stripMargin)

      log.info("Running ZIO Shield...")

      excludedZioShield.run(files)(onDiagnostic)

      if (isError) {
        throw new ZioShieldFailed()
      }
    }
}
