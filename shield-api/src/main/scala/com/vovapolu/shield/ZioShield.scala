package com.vovapolu.shield

import java.nio.file.Path

import metaconfig.Conf
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import sbt.util.Logger
import scalafix.internal.rule.DisableSyntax
import scalafix.internal.scaluzzi.DisableLegacy
import scalafix.internal.v1.Rules
import scalafix.lint.RuleDiagnostic
import scalafix.shield.ZioShieldExtension
import scalafix.v1.{Configuration, SyntacticDocument}

import scala.io.Source

object ZioShield {

  private val shieldConf: Conf = Conf
    .parseString(
      Source
        .fromInputStream(
          getClass.getClassLoader.getResourceAsStream("shield.scalafix.conf"))
        .mkString)
    .get

  private val conf = Configuration().withConf(shieldConf)

  private val all = Rules.all(this.getClass.getClassLoader)

  val syntaticRules: Rules = {
    val selectedRules = all.collect {
      case r: DisableSyntax => r
    }

    Rules(selectedRules.map(_.withConfiguration(conf).get))
  }

  val sematicRules: Rules = {
    val selectedRules = all.collect {
      case r: DisableLegacy => r
    }

    Rules(selectedRules.map(_.withConfiguration(conf).get))
  }

  def run(scalacOptions: List[String],
          files: List[Path],
          fatalWarnings: Boolean,
          logger: Logger): Unit = {

    def lintError(msg: RuleDiagnostic): String = {
      import scalafix.internal.util.PositionSyntax._
      msg.position.formatMessage(msg.severity.toString,
                                 s"[${msg.id.fullID}] ${msg.message}")

    }

    def patchError(oldDoc: String, newDoc: String, path: Path): Option[String] =
      if (oldDoc != newDoc) {
        Some(s"Detected patch for ${path.toString}:\n$newDoc")
      } else {
        None
      }

    val errors = files.flatMap { f =>
      val input = meta.Input.File(f)
      val synDoc = SyntacticDocument.fromInput(input)
      val (newDoc, msgs) =
        syntaticRules.syntacticPatch(synDoc, suppress = false)
      val synErrors =
        patchError(input.text, newDoc, f).toList ++ msgs.map(lintError)

      val semDoc = ZioShieldExtension.semanticDocumentFromPath(
        synDoc,
        f,
        scalacOptions
      )
      val semErrors = semDoc match {
        case Left(err) =>
          List(
            s"Unable to load SemanticDb information for ${f.toString}: $err. Semantic rules are disabled.")
        case Right(doc) =>
          val (newDoc, msgs) = sematicRules.semanticPatch(doc, suppress = false)
          patchError(input.text, newDoc, f).toList ++ msgs.map(lintError)
      }

      synErrors ++ semErrors
    }

    if (fatalWarnings) {
      throw new ZioShieldFailed(errors)
    } else {
      errors.foreach(e => logger.warn(e))
    }
  }
}
