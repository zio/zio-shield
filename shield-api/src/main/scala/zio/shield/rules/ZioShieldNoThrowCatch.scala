package zio.shield.rules

import scalafix.v1._
import scala.meta._

object ZioShieldNoThrowCatch extends SemanticRule("ZioShieldNoThrowCatch") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val pf: PartialFunction[Tree, Patch] = {
      case t: Term.Try   => Patch.lint(Diagnostic("", "try/catch", t.pos))
      case t: Term.Throw => Patch.lint(Diagnostic("", "throw", t.pos))
    }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
