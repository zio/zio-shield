package com.vovapolu.shield.rules

import scalafix.v1._

import scala.meta._

object ZioShieldNoPartialFunctions
    extends SemanticRule("ZioShieldNoPartialFunctions") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val throwSymbols = List(
      "scala/util/Either.LeftProjection#get().",
      "scala/util/Either.LeftProjection#get().",
      "scala/util/Either.RightProjection#get().",
      "scala/util/Try#get().",
      "scala/Option#get().",
      "scala/collection/IterableLike#head()."
    ) // TODO Java lang reflect getExceptionTypes or athrow in bytecode

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintSymbols(throwSymbols) {
        case _ => "not a total function"
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
