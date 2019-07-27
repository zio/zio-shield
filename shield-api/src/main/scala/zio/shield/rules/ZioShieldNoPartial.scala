package zio.shield.rules

import scalafix.v1._
import zio.shield.tag.{Tag, TagChecker}

import scala.meta._

class ZioShieldNoPartial(tagChecker: TagChecker)
    extends SemanticRule("ZioShieldNoPartial") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s =>
        tagChecker.check(s.value, Tag.Partial).getOrElse(false)) {
        case _ => "possible partial symbol" // TODO print proof
      } orElse {
        case l: Term.Throw =>
          Patch.lint(Diagnostic("", "not total: throwing exceptions", l.pos))
        case l: Term.Try =>
          Patch.lint(Diagnostic("", "not total: try/catch block", l.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
