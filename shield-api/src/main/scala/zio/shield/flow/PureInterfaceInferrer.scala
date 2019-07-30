package zio.shield.flow

import scalafix.v1._
import zio.shield.rules.ZioBlockDetector
import zio.shield.tag.{Tag, TagProof, TagProp}

object PureInterfaceInferrer {

  def infer(flowCache: FlowCache)(
      symbol: String): TagProp[Tag.PureInterface.type] = {
    val maybeEdge = flowCache.edges.get(symbol)

    val effectfulParents = maybeEdge match {
      case Some(ClassTraitEdge(_, parentsTypes, _)) =>
        parentsTypes.filterNot(
          flowCache.searchTag(Tag.PureInterface)(_).getOrElse(true))
      case _ => List.empty
    }

    val effectfulCtorArgs = maybeEdge match {
      case Some(ClassTraitEdge(ctorArgsTypes, _, _)) =>
        ctorArgsTypes.filterNot(
          flowCache.searchTag(Tag.PureInterface)(_).getOrElse(true))
      case _ => List.empty
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
                  case t if ZioBlockDetector.safeBlockDetector(t) =>
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
      TagProof.SymbolsProof.fromSymbols(effectfulCtorArgs),
      TagProof.PatchProof.fromPatch(constPatch)
    ).flatten

    if (proofs.nonEmpty) TagProp(Tag.PureInterface, cond = false, proofs)
    else TagProp(Tag.PureInterface, cond = true, List(TagProof.ContraryProof))
  }
}
