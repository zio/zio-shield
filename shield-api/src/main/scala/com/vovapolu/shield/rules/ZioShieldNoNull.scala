package com.vovapolu.shield.rules

import scala.meta._
import scalafix.v1._

object ZioShieldNoNull extends SemanticRule("ZioShieldNoNull") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val nullableSymbols = List(
      "java/io/File.getParent"
    ) // TODO possible can be constructed via Java reflection or bytecode analysis

    val noNullPf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintSymbols(nullableSymbols) {
        case _ => "nullable method"
      } orElse {
        case l: Lit.Null =>
          Patch.lint(Diagnostic("", "null is forbidden", l.pos))
      }

    new ZioBlockDetector(noNullPf).traverse(doc.tree)
  }
}
