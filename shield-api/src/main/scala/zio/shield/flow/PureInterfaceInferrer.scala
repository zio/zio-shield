package zio.shield.flow

import scalafix.v1._
import zio.shield.rules.ZioBlockDetector
import zio.shield.tag.{Tag, TagProof, TagProp}

import scala.meta._

case object PureInterfaceInferrer extends FlowInferrer[Tag.PureInterface.type] {

  def infer(flowCache: FlowCache)(
      symbol: String): TagProp[Tag.PureInterface.type] = {
    val maybeEdge = flowCache.edges.get(symbol)

    val effectfulParents = maybeEdge match {
      case Some(ClassTraitEdge(_, parentsTypes, _)) =>
        parentsTypes.filterNot(
          flowCache.searchTag(Tag.PureInterface)(_).getOrElse(true))
      case _ => List.empty
    }

    val traitPatch = flowCache.trees.get(symbol) match {
      case Some(t: Defn.Trait) => Patch.empty
      case Some(t) =>
        Patch.lint(Diagnostic("", "not a pure interface: not a trait", t.pos))
      case None => Patch.empty
    }

    val constPatch = maybeEdge match {
      case Some(ClassTraitEdge(_, _, innerDefns)) =>
        innerDefns.flatMap { d =>
          flowCache.edges.get(d) match {
            case Some(edge: FunctionEdge) =>
              for {
                path <- flowCache.files.get(d)
                doc <- flowCache.docs.get(path)
                tree <- flowCache.trees.get(d)
                patch = tree.collect {
                  case t if ZioBlockDetector.safeBlockDetector(t)(doc) =>
                    Patch.lint(
                      Diagnostic("", "effectful: ZIO effects usage", t.pos))
                }.asPatch
              } yield patch
            case _ => None
          }
        }.asPatch
      case _ => Patch.empty
    }

    val proofs = List(
      TagProof.SymbolsProof.fromSymbols(effectfulParents),
      TagProof.PatchProof.fromPatch(traitPatch),
      TagProof.PatchProof.fromPatch(constPatch)
    ).flatten

    if (proofs.nonEmpty) TagProp(Tag.PureInterface, cond = false, proofs)
    else TagProp(Tag.PureInterface, cond = true, List(TagProof.ContraryProof))
  }

  def dependentSymbols(edge: FlowEdge): List[String] = edge match {
    case ClassTraitEdge(_, parentsTypes, _) => parentsTypes
    case _                                  => List.empty
  }

  def isInferable(symbol: String, edge: FlowEdge): Boolean = edge match {
    case ClassTraitEdge(_, _, _) => true
    case _                       => false
  }
}
