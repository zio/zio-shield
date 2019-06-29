package zio.shield

import java.nio.file.Path

import metaconfig.Conf
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.rule.DisableSyntax
import scalafix.internal.scaluzzi.DisableLegacy
import scalafix.internal.v1.Rules
import scalafix.lint.RuleDiagnostic
import scalafix.shield.ZioShieldExtension
import scalafix.v1.{Configuration, Rule, SyntacticDocument}

import scala.io.Source

class ZioShield private (val semanticDbTargetRoot: Option[String],
                         val fullClasspath: List[Path]) {
  def apply(syntacticRules: Rules, semanticRules: Rules): ConfiguredZioShield =
    new ConfiguredZioShield(this, syntacticRules, semanticRules)

  def apply(syntacticRules: List[Rule],
            semanticRules: List[Rule]): ConfiguredZioShield =
    new ConfiguredZioShield(this, Rules(syntacticRules), Rules(semanticRules))
}

class ConfiguredZioShield(zioShieldConfig: ZioShield,
                          syntacticRules: Rules,
                          semanticRules: Rules) {

  def run(file: Path): List[ZioShieldDiagnostic] = run(List(file))

  def run(files: List[Path]): List[ZioShieldDiagnostic] = {

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
        syntacticRules.syntacticPatch(synDoc, suppress = false)
      val synErrors =
        patch(input.text, newDoc, f).toList ++ msgs.map(lint(f, _))

      val semDoc = ZioShieldExtension.semanticDocumentFromPath(
        synDoc,
        f,
        zioShieldConfig.semanticDbTargetRoot,
        zioShieldConfig.fullClasspath
      )
      val semErrors = semDoc match {
        case Left(err) =>
          List(ZioShieldDiagnostic.SemanticFailure(f, err))
        case Right(doc) =>
          val (newDoc, msgs) =
            semanticRules.semanticPatch(doc, suppress = false)
          patch(input.text, newDoc, f).toList ++ msgs.map(lint(f, _))
      }

      synErrors ++ semErrors
    }

    errors
  }
}

object ZioShield {

  def apply(semanticDbTargetRoot: Option[String],
            fullClasspath: List[Path]): ZioShield =
    new ZioShield(semanticDbTargetRoot, fullClasspath)

  def apply(scalacOptions: List[String], fullClasspath: List[Path]): ZioShield =
    new ZioShield(ZioShieldExtension.semanticdbTargetRoot(scalacOptions),
                  fullClasspath)

  private val shieldConf: Conf = Conf
    .parseString(
      Source
        .fromInputStream(
          getClass.getClassLoader.getResourceAsStream("shield.scalafix.conf"))
        .mkString)
    .get

  private val conf = Configuration().withConf(shieldConf)

  private val all = Rules.all(this.getClass.getClassLoader)

  val allSyntacticRules: Rules = {
    val selectedRules = all.collect {
      case r: DisableSyntax => r
    }

    Rules(selectedRules.map(_.withConfiguration(conf).get))
  }

  val allSemanticRules: Rules = {
    val selectedRules = all.collect {
      case r: DisableLegacy => r
    }

    Rules(selectedRules.map(_.withConfiguration(conf).get))
  }
}
