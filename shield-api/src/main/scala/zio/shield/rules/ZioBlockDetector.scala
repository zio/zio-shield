package zio.shield.rules

import scalafix.internal.scaluzzi.Disable.ContextTraverser
import scalafix.v1._
import scala.meta._

class ZioBlockDetector private (
    outsideBlock: PartialFunction[Tree, List[Patch]])(
    implicit doc: SemanticDocument) {

  import ZioBlockDetector._

  def traverse(tree: meta.Tree): Patch = {
    new ContextTraverser[List[Patch], Boolean](false)({
      case (_: Import, _) => Right(false)
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
      case (x, false) if outsideBlock.isDefinedAt(x) => Left(outsideBlock(x))
    }).result(tree).flatten.asPatch
  }
}

object ZioBlockDetector {

  def apply(outsideBlock: PartialFunction[Tree, List[Patch]])(
      implicit doc: SemanticDocument): ZioBlockDetector =
    new ZioBlockDetector(outsideBlock)

  def fromSingleLintPerTree(outsideBlock: PartialFunction[Tree, Patch])(
      implicit doc: SemanticDocument) =
    new ZioBlockDetector(outsideBlock.andThen(List(_)))

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

  val safeBlocksMatcher: SymbolMatcher =
    SymbolMatcher.normalized(safeBlocks: _*)

  val safeBlockDetector: Tree => Boolean = {
    case Term.Apply(Term.Select(safeBlocksMatcher(block), Term.Name("apply")),
                    _) =>
      true
    case Term.Apply(safeBlocksMatcher(block), _) => true
    case _                                       => false
  }

  def lintFunction(matcher: Symbol => Boolean)(
      lintMessage: PartialFunction[Symbol, String])(
      implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {

    def skipTermSelect(term: Term): Boolean = term match {
      case _: Term.Name      => true
      case Term.Select(q, _) => skipTermSelect(q)
      case _                 => false
    }

    def processName(name: Name): Option[Patch] = {
      val s = name.symbol
      if (matcher(s))
        Some(
          Patch.lint(
            Diagnostic("",
                       lintMessage.applyOrElse(s,
                                               (_: Symbol) =>
                                                 s"${s.value} is blocked"),
                       name.pos)))
      else None
    }

    {
      case Term.Select(q, name)
          if skipTermSelect(q) && processName(name).isDefined =>
        processName(name).get
      case Type.Select(q, name)
          if skipTermSelect(q) && processName(name).isDefined =>
        processName(name).get
      case name: Name if processName(name).isDefined => processName(name).get
    }
  }

  def lintSymbols(symbols: List[String])(
      lintMessage: PartialFunction[Symbol, String])(
      implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    val symbolMatcher = SymbolMatcher.normalized(symbols: _*)
    lintFunction(symbolMatcher.matches)(lintMessage)
  }

  def lintPrefixes(prefixes: List[String])(
      lintMessage: PartialFunction[Symbol, String])(
      implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    lintFunction { s =>
      prefixes.exists(s.value.startsWith)
    }(lintMessage)
  }

  def lintPrefix(prefix: String)(lintMessage: PartialFunction[Symbol, String])(
      implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    lintPrefixes(List(prefix))(lintMessage)
  }
}
