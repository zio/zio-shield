package zio.shield.flow

import scalafix.v1._
import zio.shield.tag._

import scala.io.{ Source => ScalaSource }

import scala.meta._

case object PartialityInferrer extends FlowInferrer[Tag.Partial.type] {

  val constPartialMatcher: SymbolMatcher = SymbolMatcher.normalized(
    List(
      "scala.util.Either.LeftProjection.get",
      "scala.util.Either.LeftProjection.get",
      "scala.util.Either.RightProjection.get",
      "scala.util.Try.get",
      "scala.Option.get",
      "scala.collection.IterableLike"
    ) ++ ScalaSource
      .fromInputStream(getClass.getClassLoader.getResourceAsStream("partial_methods.txt"))
      .getLines()
      .toList: _*
  )

  val name: String = toString

  def infer(flowCache: FlowCache)(symbol: String): TagProp[Tag.Partial.type] =
    if (PartialityInferrer.constPartialMatcher.matches(Symbol(symbol))) {
      TagProp(Tag.Partial, cond = true, List(TagProof.GivenProof))
    } else {

      val constPatch = flowCache.trees.get(symbol) match {
        case Some(tree) =>
          tree.collect {
            case l: Term.Throw =>
              Patch.lint(Diagnostic("", "not total: throwing exceptions", l.pos))
            case l: Term.Try =>
              Patch.lint(Diagnostic("", "not total: try/catch block", l.pos))
          }.asPatch
        case None => Patch.empty
      }

      val partialSymbols = flowCache.edges.get(symbol) match {
        case Some(FunctionEdge(_, _, innerSymbols)) =>
          innerSymbols.filter(flowCache.searchTag(Tag.Partial)(_).getOrElse(false))
        case Some(ValVarEdge(innerSymbols)) =>
          innerSymbols.filter(flowCache.searchTag(Tag.Partial)(_).getOrElse(false))
        case _ => List.empty
      }

      val proofs = List(
        TagProof.PatchProof.fromPatch(constPatch),
        TagProof.SymbolsProof.fromSymbols(partialSymbols)
      ).flatten

      if (proofs.nonEmpty) TagProp(Tag.Partial, cond = true, proofs)
      else TagProp(Tag.Partial, cond = false, List(TagProof.ContraryProof))
    }

  def dependentSymbols(edge: FlowEdge): List[String] = edge match {
    case FunctionEdge(_, _, innerSymbols)      => innerSymbols
    case ValVarEdge(innerSymbols)              => innerSymbols
    case ClassTraitEdge(_, _, _, innerSymbols) => innerSymbols
    case ObjectEdge(_, innerSymbols)           => innerSymbols
    case _                                     => List.empty
  }

  def isInferable(symbol: String, edge: FlowEdge): Boolean =
    edge match {
      case FunctionEdge(_, _, _) => true
      case ValVarEdge(_)         => true
      case _                     => false
    }
}
