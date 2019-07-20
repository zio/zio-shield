package zio.shield

import java.nio.file.Path

import metaconfig.Conf
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.rule.DisableSyntax
import scalafix.internal.scaluzzi.DisableLegacy
import scalafix.internal.v1.Rules
import scalafix.lint.RuleDiagnostic
import scalafix.shield.ZioShieldExtension
import scalafix.v1.{Configuration, SyntacticDocument}
import zio.shield.flow.{FlowCache, FlowCacheTagCheckerImpl}
import zio.shield.rules.{ZioShieldRule, ZioShieldRules}
import zio.shield.tag.TagChecker

import scala.io.Source

class ZioShield private (val semanticDbTargetRoot: Option[String],
                         val fullClasspath: List[Path]) {
  def apply(syntacticRules: ZioShieldRules,
            semanticRules: ZioShieldRules): ConfiguredZioShield =
    new ConfiguredZioShield(this, syntacticRules, semanticRules)

  def apply(syntacticRules: List[ZioShieldRule],
            semanticRules: List[ZioShieldRule]): ConfiguredZioShield =
    new ConfiguredZioShield(this,
                            ZioShieldRules(syntacticRules),
                            ZioShieldRules(semanticRules))
}

class ConfiguredZioShield(zioShieldConfig: ZioShield,
                          syntacticRules: ZioShieldRules,
                          semanticRules: ZioShieldRules) {

  private val flowCache = FlowCache.empty

  val tagChecker: TagChecker = new FlowCacheTagCheckerImpl(flowCache)

  def run(file: Path): List[ZioShieldDiagnostic] = run(List(file))

  def run(files: List[Path]): List[ZioShieldDiagnostic] = {

    val inputs = files.map(meta.Input.File(_))

    val synDocs = inputs.map { i =>
      i.path.toNIO -> SyntacticDocument.fromInput(i)
    }.toMap

    val (semFailErrors, semDocs) = {
      val docsOrErrors = inputs.map { i =>
        val synDoc = synDocs(i.path.toNIO)
        i.path.toNIO -> ZioShieldExtension.semanticDocumentFromPath(
          synDoc,
          i.path.toNIO,
          zioShieldConfig.semanticDbTargetRoot,
          zioShieldConfig.fullClasspath
        )
      }
      val errors = docsOrErrors.collect {
        case (path, Left(err)) => ZioShieldDiagnostic.SemanticFailure(f, err)
      }
      val docs = docsOrErrors.collect {
        case (path, Right(doc)) => path -> doc
      }

      (errors, docs.toMap)
    }

    flowCache.build(semDocs)

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

    val errors = inputs.flatMap { input =>
      val path = input.path.toNIO
      val synDoc = synDocs(path)
      val semDoc = semDocs(path)

      val (newDoc, msgs) =
        syntacticRules.syntacticPatch(synDoc, suppress = false)
      val synErrors =
        patch(input.text, newDoc, path).toList ++ msgs.map(lint(path, _))

      val semErrors = {
        val (newDoc, msgs) =
          semanticRules.semanticPatch(semDoc, suppress = false)
        patch(input.text, newDoc, path).toList ++ msgs.map(lint(path, _))
      }

      semFailErrors ++ synErrors ++ semErrors
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
