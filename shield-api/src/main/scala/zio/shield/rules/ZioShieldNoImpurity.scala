package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.ImpurityInferrer
import zio.shield.tag.{Tag, TagChecker}

import scala.meta._

class ZioShieldNoImpurity(tagChecker: TagChecker)
    extends SemanticRule("ZioShieldNoImpurity") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s =>
        tagChecker.check(s.value, Tag.Impure).getOrElse(false)) {
        case _ => "possibly impure" // TODO print proof
      } orElse ImpurityInferrer.constImpurityChecker

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
