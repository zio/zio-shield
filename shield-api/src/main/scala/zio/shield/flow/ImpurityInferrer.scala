package zio.shield.flow

import scalafix.v1._
import zio.shield.tag._

import scala.meta._

case object ImpurityInferrer extends FlowInferrer[Tag.Impure.type] {

  val constImpureSymbols = List(
    "scala/Predef.println(+1).",
    "scala/Predef.println()."
  ) // TODO possible can be constructed via Java reflection or bytecode analysis

  def constImpurityChecker(
      implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {
    def skipTermSelect(term: Term): Boolean = term match {
      case _: Term.Name      => true
      case Term.Select(q, _) => skipTermSelect(q)
      case _                 => false
    }

    def isUnitMethod(s: Symbol): Boolean = {
      s.info.map(_.signature) match {
        case Some(MethodSignature(_, _, TypeRef(_, s: Symbol, List())))
            if s.value == "scala/Unit#" =>
          true
        case _ => false
      }
    }

    {
      case Term.Select(q, name)
          if skipTermSelect(q) && isUnitMethod(name.symbol) =>
        Patch.lint(Diagnostic("", "impure: calling unit method", name.pos))
      case Type.Select(q, name)
          if skipTermSelect(q) && isUnitMethod(name.symbol) =>
        Patch.lint(Diagnostic("", "impure: calling unit method", name.pos))
      case t: Term.Name if isUnitMethod(t.symbol) =>
        Patch.lint(Diagnostic("", "impure: calling unit method", t.pos))
    }
  }

  def infer(flowCache: FlowCache)(symbol: String): TagProp[Tag.Impure.type] = {
    if (ImpurityInferrer.constImpureSymbols.contains(symbol)) {
      TagProp(Tag.Impure, cond = true, List(TagProof.GivenProof))
    } else {

      val constPatch = flowCache.trees.get(symbol) match {
        case Some(tree) =>
          val maybePatch = for {
            path <- flowCache.files.get(symbol)
            doc <- flowCache.docs.get(path)
            patch = tree.collect(constImpurityChecker(doc)).asPatch
          } yield patch

          maybePatch.getOrElse(Patch.empty)
        case None => Patch.empty
      }

      val impureSymbols = flowCache.edges.get(symbol) match {
        case Some(FunctionEdge(_, _, innerSymbols)) =>
          innerSymbols.filter(
            flowCache.searchTag(Tag.Impure)(_).getOrElse(false))
        case Some(ValVarEdge(innerSymbols)) =>
          innerSymbols.filter(
            flowCache.searchTag(Tag.Impure)(_).getOrElse(false))
        case _ => List.empty
      }

      val proofs = List(
        TagProof.PatchProof.fromPatch(constPatch),
        TagProof.SymbolsProof.fromSymbols(impureSymbols)
      ).flatten

      if (proofs.nonEmpty) TagProp(Tag.Impure, cond = true, proofs)
      else TagProp(Tag.Impure, cond = false, List(TagProof.ContraryProof))
    }
  }
}
