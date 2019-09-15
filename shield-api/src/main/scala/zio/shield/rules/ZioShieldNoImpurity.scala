package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.{FlowCache, FlowInferrer, ImpurityInferrer}
import zio.shield.tag.Tag

import scala.meta._

object ZioShieldNoImpurity extends FlowRule {
  val name = "ZioShieldNoImpurity"

  val dependsOn = List(ImpurityInferrer)

  def fix(cache: FlowCache)(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s =>
        cache.searchTag(Tag.Impure)(s.value).getOrElse(false)) {
        case _ => "possibly impure" // TODO print proof
      } //orElse ImpurityInferrer.constImpurityChecker

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
