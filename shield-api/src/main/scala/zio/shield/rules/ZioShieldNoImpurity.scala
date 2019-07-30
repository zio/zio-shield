package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.{FlowCache, ImpurityInferrer}
import zio.shield.tag.Tag

import scala.meta._

class ZioShieldNoImpurity(cache: FlowCache)
    extends SemanticRule("ZioShieldNoImpurity") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s =>
        cache.searchTag(Tag.Impure)(s.value).getOrElse(false)) {
        case _ => "possibly impure" // TODO print proof
      } orElse ImpurityInferrer.constImpurityChecker

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
