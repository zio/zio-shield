package com.vovapolu.shield

import java.nio.file.Path

import metaconfig.Conf
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
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
          files: List[Path]): List[ZioShieldDiagnostic] =
    run(ZioShieldExtension.semanticdbTargetRoot(scalacOptions), files)

  def run(semanticDbTargetRoot: Option[String],
          files: List[Path]): List[ZioShieldDiagnostic] = {

    def lint(path: Path, msg: RuleDiagnostic): ZioShieldDiagnostic =
      ZioShieldDiagnostic.Lint(path, msg.position, msg.message)

    def patch(oldDoc: String,
              newDoc: String,
              path: Path): Option[ZioShieldDiagnostic] =
      if (oldDoc != newDoc) {
        Some(ZioShieldDiagnostic.Patch(path, oldDoc, newDoc))
      } else {
        None
      }

    val errors = files.flatMap { f =>
      val input = meta.Input.File(f)
      val synDoc = SyntacticDocument.fromInput(input)
      val (newDoc, msgs) =
        syntaticRules.syntacticPatch(synDoc, suppress = false)
      val synErrors =
        patch(input.text, newDoc, f).toList ++ msgs.map(lint(f, _))

      val semDoc = ZioShieldExtension.semanticDocumentFromPath(
        synDoc,
        f,
        semanticDbTargetRoot
      )
      val semErrors = semDoc match {
        case Left(err) =>
          List(ZioShieldDiagnostic.SemanticFailure(f, err))
        case Right(doc) =>
          val (newDoc, msgs) = sematicRules.semanticPatch(doc, suppress = false)
          patch(input.text, newDoc, f).toList ++ msgs.map(lint(f, _))
      }

      synErrors ++ semErrors
    }

    errors
  }
}
