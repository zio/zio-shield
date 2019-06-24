package com.vovapolu.shield.rules

import scalafix.v1._

import scala.meta._
import scala.util.Try

object ZioShieldNoSideEffects extends SemanticRule("ZioShieldNoSideEffects") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val sideEffectsSymbols = List(
      "scala/Predef.println(+1).",
      "scala/Predef.println()."
    )

    def skipTermSelect(term: Term): Boolean = term match {
      case _: Term.Name      => true
      case Term.Select(q, _) => skipTermSelect(q)
      case _                 => false
    }

    def isUnitMethod(s: Symbol): Boolean = {
      //println(s.value)
      //println(Try(s.info).toOption.flatten.map(_.signature.structure))
      Try(s.info).toOption.flatten.map(_.signature) match {
        case Some(MethodSignature(_, _, TypeRef(_, s: Symbol, List())))
            if s.value == "scala/Unit#" =>
          true
        case _ => false
      }
    }

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintSymbols(sideEffectsSymbols) {
        case _ => "possible side-effect"
      } orElse {
        case Term.Select(q, name)
            if skipTermSelect(q) && isUnitMethod(name.symbol) =>
          Patch.lint(Diagnostic("", "calling unit method", name.pos))
        case Type.Select(q, name)
            if skipTermSelect(q) && isUnitMethod(name.symbol) =>
          Patch.lint(Diagnostic("", "calling unit method", name.pos))
        case t: Term.Name if isUnitMethod(t.symbol) =>
          Patch.lint(Diagnostic("", "calling unit method", t.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
