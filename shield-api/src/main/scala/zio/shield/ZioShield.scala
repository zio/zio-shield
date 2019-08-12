package zio.shield

import java.nio.file.Path

import metaconfig.Conf
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.v1.Rules
import scalafix.lint.RuleDiagnostic
import scalafix.shield.ZioShieldExtension
import scalafix.v1._
import zio.shield.flow._
import zio.shield.rules._

import scala.io.Source

class ZioShield private (val semanticDbTargetRoot: Option[String],
                         val fullClasspath: List[Path]) {

  val flowCache: FlowCache = FlowCache.empty

  def apply(syntacticRules: List[Rule] = List.empty,
            semanticRules: List[Rule] = List.empty,
            semanticZioShieldRules: List[
              FlowCache => Rule with FlowInferenceDependent] = List.empty)
    : ConfiguredZioShield =
    new ConfiguredZioShield(
      this,
      Rules(syntacticRules),
      Rules(semanticRules ++ semanticZioShieldRules.map(_(flowCache))))

  def withAllRules(): ConfiguredZioShield =
    apply(List.empty, ZioShield.allSemanticRules, ZioShield.allZioShieldRules)
}

class ConfiguredZioShield(val zioShieldConfig: ZioShield,
                          val syntacticRules: Rules,
                          val semanticRules: Rules) {

  lazy val inferrers: List[FlowInferrer[_]] =
    semanticRules.rules
      .collect {
        case flowDependent: FlowInferenceDependent => flowDependent.dependsOn
      }
      .flatten
      .distinct

  def exclude(
      excludedRules: List[String] = List.empty,
      excludedInferrers: List[String] = List.empty): ConfiguredZioShield =
    new ConfiguredZioShield(
      zioShieldConfig,
      Rules(
        syntacticRules.rules.filterNot(r =>
          excludedRules.contains(r.name.value)),
      ),
      Rules(
        semanticRules.rules.filterNot {
          case flowDependent: FlowInferenceDependent =>
            excludedInferrers.exists(ei =>
              flowDependent.dependsOn.exists(_.name == ei)) ||
              excludedRules.contains(flowDependent.name.value)
          case r => excludedRules.contains(r.name.value)
        }
      )
    )

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
        case (path, Left(err)) => ZioShieldDiagnostic.SemanticFailure(path, err)
      }
      val docs = docsOrErrors.collect {
        case (path, Right(doc)) => path -> doc
      }

      (errors, docs.toMap)
    }

    zioShieldConfig.flowCache.build(semDocs)
    zioShieldConfig.flowCache.infer(inferrers)

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

      val synErrors = synDocs.get(path) match {
        case Some(synDoc) =>
          val (newDoc, msgs) =
            syntacticRules.syntacticPatch(synDoc, suppress = false)
          patch(input.text, newDoc, path).toList ++ msgs.map(lint(path, _))
        case None => List.empty
      }

      val semErrors = semDocs.get(path) match {
        case Some(semDoc) =>
          val (newDoc, msgs) =
            semanticRules.semanticPatch(semDoc, suppress = false)
          patch(input.text, newDoc, path).toList ++ msgs.map(lint(path, _))
        case None => List.empty
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

  @deprecated
  private val shieldConf: Conf = Conf
    .parseString(
      Source
        .fromInputStream(
          getClass.getClassLoader.getResourceAsStream("shield.scalafix.conf"))
        .mkString)
    .get

  @deprecated
  private val conf = Configuration().withConf(shieldConf)

  @deprecated
  private val all = Rules.all(this.getClass.getClassLoader)

  val allSemanticRules = List(ZioShieldNoFutureMethods,
                              ZioShieldNoIgnoredExpressions,
                              ZioShieldNoReflection,
                              ZioShieldNoTypeCasting)

  val allZioShieldRules: List[FlowCache => Rule with FlowInferenceDependent] =
    List(
      new ZioShieldNoImpurity(_),
      new ZioShieldNoIndirectUse(_),
      new ZioShieldNoNull(_),
      new ZioShieldNoPartial(_)
    )
}
