package com.vovapolu.shield

import java.nio.file.Path

import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

object ZioShieldPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin

  object autoImport {
    val shield: TaskKey[Unit] =
      taskKey[Unit]("Run ZIO Shield")

    def scalafixConfigSettings(config: Configuration): Seq[Def.Setting[_]] =
      Seq(
        shield := shieldTask(config).value
      )
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test).flatMap(c => inConfig(c)(scalafixConfigSettings(c)))

  private def shieldTask(
      config: Configuration
  ): Def.Initialize[Task[Unit]] =
    Def.task {
      ZioShield.run(scalacOptions.in(config).value.toList,
                    unmanagedSources.in(config).value.map(_.toPath).toList,
                    streams.value.log)
    }

  private def validateProject(
      files: Seq[Path],
      dependencies: Seq[ModuleID],
      ruleNames: Seq[String]
  ): Option[String] = {
    if (files.isEmpty) None
    else {
      val isSemanticdb =
        dependencies.exists(_.name.startsWith("semanticdb-scalac"))
      if (!isSemanticdb) {
        val names = ruleNames.mkString(", ")
        Some(
          s"""|The semanticdb-scalac compiler plugin is required to run semantic rules like $names.
              |To fix this problem for this sbt shell session, run `scalafixEnable` and try again.
              |To fix this problem permanently for your build, add the following settings to build.sbt:
              |  addCompilerPlugin(scalafixSemanticdb)
              |  scalacOptions += "-Yrangepos"
              |""".stripMargin)
      } else None
    }
  }

}
