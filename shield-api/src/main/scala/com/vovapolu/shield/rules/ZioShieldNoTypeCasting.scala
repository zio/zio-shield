package com.vovapolu.shield.rules

import scala.meta._
import scalafix.v1._

object ZioShieldNoTypeCasting extends SemanticRule("ZioShieldNoTypeCasting") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val typeCastingSymbols = List(
      "scala/Any#asInstanceOf().",
      "scala/Any#isInstanceOf()."
    )

    val noTypeCastingPf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintSymbols(typeCastingSymbols) {
        case _ => "type casting"
      } // TODO Also add pattern match detection

    new ZioBlockDetector(noTypeCastingPf).traverse(doc.tree)
  }
}
