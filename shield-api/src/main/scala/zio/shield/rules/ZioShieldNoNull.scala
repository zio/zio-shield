package zio.shield.rules

import scala.meta._
import scalafix.v1._
import zio.shield.tag.{Tag, TagChecker}

class ZioShieldNoNull(tagChecker: TagChecker)
    extends SemanticRule("ZioShieldNoNull") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s =>
        tagChecker.check(s.value, Tag.Nullable).getOrElse(false)) {
        case _ => "possibly nullable" // TODO print proof
      } orElse {
        case l: Lit.Null =>
          Patch.lint(Diagnostic("", "nullable: null usage", l.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
