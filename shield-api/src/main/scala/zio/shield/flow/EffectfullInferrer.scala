package zio.shield.flow

import scalafix.v1._
import zio.shield.rules.ZioBlockDetector
import zio.shield.tag.{ Tag, TagProof, TagProp }

import scala.meta._

object EffectfullInferrer extends FlowInferrer[Tag.Effectful.type] {

  val name: String = toString

  def infer(flowCache: FlowCache)(symbol: String): TagProp[Tag.Effectful.type] = {
    val constPatch =
      (for {
        path <- flowCache.files.get(symbol)
        doc  <- flowCache.docs.get(path)
        tree <- flowCache.trees.get(symbol)
        patch = tree.collect {
          case t if ZioBlockDetector.safeBlockDetector(t)(doc) =>
            Patch.lint(Diagnostic("", "effectful: ZIO effects usage outside of pure interface", t.pos))
        }.asPatch
      } yield patch).getOrElse(Patch.empty)

    val effectfulSymbol = flowCache.edges.get(symbol) match {
      case Some(FunctionEdge(_, _, innerSymbols)) =>
        innerSymbols.filter(flowCache.searchTag(Tag.Effectful)(_).getOrElse(false))
      case Some(ValVarEdge(innerSymbols)) =>
        innerSymbols.filter(flowCache.searchTag(Tag.Effectful)(_).getOrElse(false))
      case _ => List.empty
    }

    val proofs = List(
      TagProof.PatchProof.fromPatch(constPatch),
      TagProof.SymbolsProof.fromSymbols(effectfulSymbol)
    ).flatten

    if (proofs.nonEmpty) TagProp(Tag.Effectful, cond = true, proofs)
    else TagProp(Tag.Effectful, cond = false, List(TagProof.ContraryProof))
  }

  def dependentSymbols(edge: FlowEdge): List[String] = edge match {
    case FunctionEdge(_, _, innerSymbols) => innerSymbols
    case ValVarEdge(innerSymbols)         => innerSymbols
    case _                                => List.empty
  }

  def isInferable(symbol: String, edge: FlowEdge): Boolean = edge match {
    case FunctionEdge(_, _, _) => true
    case ValVarEdge(_)         => true
    case _                     => false
  }
}
