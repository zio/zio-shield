package com.vovapolu.shield.rules

import scalafix.v1._
import scala.meta._

object ZioShieldNoThrowCatch extends SemanticRule("ZioShieldNoThrowCatch") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val noThrowCatchPf: PartialFunction[Tree, Patch] = {
      case t: Term.Try   => Patch.lint(Diagnostic("", "try/catch", t.pos))
      case t: Term.Throw => Patch.lint(Diagnostic("", "throw", t.pos))
    }

    new ZioBlockDetector(noThrowCatchPf).traverse(doc.tree)
  }
}
