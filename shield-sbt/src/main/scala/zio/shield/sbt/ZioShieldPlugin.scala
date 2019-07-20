package zio.shield.sbt

import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}
import zio.shield.ZioShield

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

    def shieldConfigSettings(config: Configuration): Seq[Def.Setting[_]] =
      Seq(
        shield := (shieldTask(config) dependsOn (compile in config)).value
      )
  }

  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    shieldFatalWarnings := false
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      libraryDependencies ++= Seq(
        compilerPlugin(
          "org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full),
        compilerPlugin("zio.shield" %% "zio-shield-scalac" % "0.3.0-SNAPSHOT")),
      scalacOptions += "-Yrangepos"
    ) ++
      Seq(Compile, Test).flatMap(c => inConfig(c)(shieldConfigSettings(c)))

  private def shieldTask(
      config: Configuration
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      val zioShield = ZioShield(scalacOptions.in(config).value.toList,
                                fullClasspath.value.map(_.data.toPath).toList)(
        ZioShield.allSyntacticRules,
        ZioShield.allSemanticRules)

      val errors =
        zioShield.run(unmanagedSources.in(config).value.map(_.toPath).toList)

      val log = streams.value.log

      if (shieldFatalWarnings.value) {
        throw new ZioShieldFailed(errors.map(_.consoleMessage))
      } else {
        errors.foreach(e => log.warn(e.consoleMessage))
      }
    }
}
