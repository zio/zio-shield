package zio.shield.flow

import zio.shield.tag.{Tag, TagProof, TagProp}

case object ImplementationInferrer
    extends FlowInferrer[Tag.Implementaion.type] {

  def infer(flowCache: FlowCache)(
      symbol: String): TagProp[Tag.Implementaion.type] = {

    val pureInterfaceOrImplementationParents =
      flowCache.edges.get(symbol) match {
        case Some(ClassTraitEdge(_, parentsTypes, _)) =>
          parentsTypes.filter { p =>
            (for {
              pure <- flowCache.searchTag(Tag.PureInterface)(p)
              impl <- flowCache.searchTag(Tag.Implementaion)(p)
            } yield pure || impl).getOrElse(false)
          }
        case _ => List.empty
      }

    val proofs = List(
      TagProof.SymbolsProof.fromSymbols(pureInterfaceOrImplementationParents),
    ).flatten

    if (proofs.nonEmpty) TagProp(Tag.Implementaion, cond = true, proofs)
    else TagProp(Tag.Implementaion, cond = false, List(TagProof.ContraryProof))
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
