package com.vovapolu.shield.rules

import scalafix.internal.scaluzzi.Disable.ContextTraverser

import scala.meta._
import scalafix.v1._

class ZioShieldNoNull extends SemanticRule("ZioShieldNoNull") {

  val safeBlocks = List(
    "zio/Task",
    "zio/IO",
    "zio/UIO",
    "zio/ZIO",
    "zio/ZIO_E_Throwable.effect",
    "zio/ZIOFunctions.effectAsync",
    "zio/ZIOFunctions.effectTotal",
    "zio/ZIOFunctions.effectTotalWith",
    "zio/ZIOFunctions.fail",
    "zio/ZIOFunctions.halt"
  )

  private val safeBlocksMatcher = SymbolMatcher.normalized(safeBlocks: _*)

  val nullableSymbols = List(
    "java/io/File.getParent"
  )

  private val nullableMatcher = SymbolMatcher.normalized(nullableSymbols: _*)

  override def fix(implicit doc: SemanticDocument): Patch = {

    def processName(name: Name): Option[Patch] =
      if (nullableMatcher.matches(name.symbol))
        Some(Patch.lint(Diagnostic("", "Nullable method", name.pos)))
      else None

    def skipTermSelect(term: Term): Boolean = term match {
      case _: Term.Name      => true
      case Term.Select(q, _) => skipTermSelect(q)
      case _                 => false
    }

    new ContextTraverser[Patch, Boolean](false)({
      case (_: Import, _) => Right(false)
      case (Term.Select(q, name), isSafe) if skipTermSelect(q) =>
        if (!isSafe) processName(name).toLeft(isSafe)
        else Right(isSafe)
      case (Type.Select(q, name), isSafe) if skipTermSelect(q) =>
        if (!isSafe) processName(name).toLeft(isSafe)
        else Right(isSafe)
      case (Term.Apply(
              Term.Select(safeBlocksMatcher(block), Term.Name("apply")),
              _),
            _) =>
        Right(true) // <Block>.apply
      case (Term.Apply(safeBlocksMatcher(block), _), _) =>
        Right(true) // <Block>(...)
      case (_: Defn.Def, _) =>
        Right(false) // reset blocked symbols in def
      case (_: Term.Function, _) =>
        Right(false) // reset blocked symbols in (...) => (...)
      case (name: Name, isSafe) =>
        if (!isSafe) processName(name).toLeft(isSafe)
        else Right(isSafe)
      case (l: Lit.Null, isSafe) =>
        Left(Patch.lint(Diagnostic("", "null is forbidden", l.pos)))
    }).result(doc.tree).asPatch
  }
}
