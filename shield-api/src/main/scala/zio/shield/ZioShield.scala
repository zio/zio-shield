package zio.shield

import java.nio.file.Path

import scalafix.internal.v1.Rules
import scalafix.lint.RuleDiagnostic
import scalafix.v1._
import zio.shield.config.{ Config, InvalidConfig }
import zio.shield.flow._
import zio.shield.rules._

import scala.collection.mutable

trait SemanticDocumentLoader {
  def load(synDoc: SyntacticDocument, path: Path): Either[Throwable, SemanticDocument]
}

class ZioShield private (val loader: SemanticDocumentLoader) {

  def withExcluded(
    excludedRules: List[String] = List.empty,
    excludedInferrers: List[String] = List.empty
  ): ConfiguredZioShield = {

    val filteredSemanticRules = ZioShield.allSemanticRules.filterNot(r => excludedRules.contains(r.name.value))
    val filteredFlowRules = ZioShield.allFlowRules.filterNot(r =>
      excludedInferrers.exists(ei => r.dependsOn.exists(_.name == ei)) ||
        excludedRules.contains(r.name)
    )

    ConfiguredZioShield(loader)(List.empty, filteredSemanticRules, filteredFlowRules)
  }

  def withAllRules(): ConfiguredZioShield =
    ConfiguredZioShield(loader)(List.empty, ZioShield.allSemanticRules, ZioShield.allFlowRules)

  def withConfig(config: Config): ConfiguredZioShield =
    withExcluded(config.excludedRules, config.excludedInferrers)
}

final case class ConfiguredZioShield(loader: SemanticDocumentLoader)(
  syntacticRules: List[Rule],
  semanticRules: List[Rule],
  flowRules: List[FlowRule]
) {

  private val flowCache: FlowCache = FlowCache.empty

  private val combinedSyntacticRules = Rules(syntacticRules)
  private val combinedSemanticRules  = Rules(semanticRules ++ flowRules.map(_.toRule(flowCache)))

  private val synDocs: mutable.Map[Path, SyntacticDocument] =
    mutable.HashMap.empty
  private val semDocs: mutable.Map[Path, SemanticDocument] =
    mutable.HashMap.empty

  lazy val inferrers: List[FlowInferrer[_]] =
    flowRules.flatMap(_.dependsOn).distinct

  def updateCache(files: List[Path])(onDiagnostic: ZioShieldDiagnostic => Unit): Unit = {
    val inputs = files.map(meta.Input.File(_))

    inputs.foreach { i =>
      synDocs.update(i.path.toNIO, SyntacticDocument.fromInput(i))
    }

    inputs.foreach { i =>
      val path   = i.path.toNIO
      val synDoc = synDocs(path)
      loader.load(
        synDoc,
        i.path.toNIO
      ) match {
        case Left(err) =>
          onDiagnostic(ZioShieldDiagnostic.SemanticFailure(path, err))
        case Right(doc) => semDocs.update(path, doc)
      }
    }

    flowCache.update(files.flatMap(f => semDocs.get(f).map(f -> _)).toMap)
    flowCache.deepInferAndCache(inferrers)(files)
  }

  def cacheStats: FlowCache.Stats = flowCache.stats

  def run(files: List[Path])(onDiagnostic: ZioShieldDiagnostic => Unit): Unit = {

    val inputs = files.map(meta.Input.File(_))

    def lint(path: Path, msg: RuleDiagnostic): ZioShieldDiagnostic =
      ZioShieldDiagnostic.Lint(path, msg.position, msg.message)

    def patch(oldDoc: String, newDoc: String, path: Path): Option[ZioShieldDiagnostic] =
      if (oldDoc != newDoc) {
        Some(ZioShieldDiagnostic.Patch(path, oldDoc, newDoc))
      } else {
        None
      }

    inputs.foreach { input =>
      val path = input.path.toNIO

      synDocs.get(path) match {
        case Some(synDoc) =>
          val (newDoc, msgs) =
            combinedSyntacticRules.syntacticPatch(synDoc, suppress = false)
          patch(input.text, newDoc, path).foreach(onDiagnostic)
          msgs.foreach(m => onDiagnostic(lint(path, m)))
        case None =>
      }

      semDocs.get(path) match {
        case Some(semDoc) =>
          val (newDoc, msgs) =
            combinedSemanticRules.semanticPatch(semDoc, suppress = false)
          patch(input.text, newDoc, path).foreach(onDiagnostic)
          msgs.foreach(m => onDiagnostic(lint(path, m)))
        case None =>
      }
    }
  }
}

object ZioShield {

  def apply(loader: SemanticDocumentLoader): ZioShield = new ZioShield(loader)

  val allSemanticRules =
    List(ZioShieldNoFutureMethods, ZioShieldNoIgnoredExpressions, ZioShieldNoReflection, ZioShieldNoTypeCasting)

  val allFlowRules: List[FlowRule] =
    List(
      ZioShieldNoImpurity,
      ZioShieldNoIndirectUse,
      ZioShieldNoNull,
      ZioShieldNoPartial
    )

  val allInferrers: List[FlowInferrer[_]] = List(
    EffectfullInferrer,
    ImplementationInferrer,
    ImpurityInferrer,
    NullabilityInferrer,
    PartialityInferrer,
    PureInterfaceInferrer
  )

  def validateConfig(config: Config): Option[Throwable] = {
    val invalidRules = config.excludedRules.filterNot(r =>
      allSemanticRules.exists(_.name.value == r) ||
        allFlowRules.exists(_.name == r)
    )

    val invalidInferrers =
      config.excludedInferrers.filterNot(i => allInferrers.exists(_.name == i))

    if (invalidRules.nonEmpty || invalidInferrers.nonEmpty) {
      val msg = List(
        if (invalidRules.nonEmpty)
          Some(s"invalid rules: ${invalidRules.mkString(", ")}")
        else None,
        if (invalidInferrers.nonEmpty)
          Some(s"invalid inferrers: ${invalidInferrers.mkString(", ")}")
        else None
      ).flatten.mkString(", ")
      Some(new InvalidConfig(msg))
    } else {
      None
    }
  }
}
