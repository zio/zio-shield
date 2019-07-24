package zio.shield.flow

import scalafix.v1._
import zio.shield.tag._

import scala.collection.mutable
import scala.meta._

case object PartialityInferrer extends FlowInferrer[Tag.Total.type] {

  val constPartialSymbols = List(
    "scala/util/Either.LeftProjection#get().",
    "scala/util/Either.LeftProjection#get().",
    "scala/util/Either.RightProjection#get().",
    "scala/util/Try#get().",
    "scala/Option#get().",
    "scala/collection/IterableLike#head()."
  ) // TODO possible can be constructed via Java reflection or bytecode analysis

  def infer(flowCache: FlowCache)(symbol: String): TagProp[Tag.Total.type] = {
    if (PartialityInferrer.constPartialSymbols.contains(symbol)) {
      TagProp(Tag.Total, cond = false, List(TagProof.GivenProof))
    } else {

      val constPatch = flowCache.trees.get(symbol) match {
        case Some(tree) =>
          tree.collect {
            case l: Term.Throw =>
              Patch.lint(
                Diagnostic("", "not total: throwing exceptions", l.pos))
            case l: Term.Try =>
              Patch.lint(Diagnostic("", "not total: try/catch block", l.pos))
          }.asPatch
        case None => Patch.empty
      }

      def primitivePartialSearch(symbols: List[String]): List[String] =
        symbols.filter { s =>
          def findProp(tags: mutable.Map[String, mutable.Buffer[TagProp[_]]])
            : Option[Boolean] =
            tags
              .getOrElse(s, mutable.Buffer.empty)
              .find(p => p.tag == Tag.Total && p.isProved)
              .map(_.cond)

          lazy val userProp = findProp(flowCache.userTags)

          lazy val inferredProp = findProp(flowCache.inferredTags)

          !userProp.orElse(inferredProp).getOrElse(true)
        }

      val partialSymbols = flowCache.symbols.get(symbol) match {
        case Some(FunctionEdge(_, _, innerSymbols)) =>
          primitivePartialSearch(innerSymbols)
        case Some(ValVarEdge(innerSymbols)) =>
          primitivePartialSearch(innerSymbols)
        case _ => List.empty
      }

      val proofs = List(
        TagProof.PatchProof.fromPatch(constPatch),
        TagProof.SymbolsProof.fromSymbols(partialSymbols)
      ).flatten

      if (proofs.nonEmpty) TagProp(Tag.Total, cond = false, proofs)
      else TagProp(Tag.Total, cond = true, List(TagProof.ContraryProof))
    }
  }
}
