package zio.shield.flow

import scalafix.v1._
import scala.meta._
import zio.shield.tag._

import scala.collection.mutable

class NullableInferrer(val flowCache: FlowCache)
    extends FlowInferrer[Tag.Nullable.type] {

  def infer(symbol: String): TagProp[Tag.Nullable.type] = {
    if ()

    val constPatch = flowCache.trees.get(symbol) match {
      case Some(tree) =>
        tree.collect {
          case l: Lit.Null =>
            Patch.lint(Diagnostic("", "null is forbidden", l.pos))
        }.asPatch
      case None => Patch.empty
    }

    def primitiveNullableSearch(symbols: List[String]): List[String] =
      symbols.filter { s =>
        def findProp(tags: mutable.Map[String, mutable.Buffer[TagProp[_]]])
          : Option[Boolean] =
          tags
            .getOrElse(s, mutable.Buffer.empty)
            .collectFirst {
              case p @ TagProp(Tag.Nullable, cond, _) if p.isProved => cond
            }

        lazy val userProp = findProp(flowCache.userTags)

        lazy val inferredProp = findProp(flowCache.inferredTags)

        userProp.orElse(inferredProp).getOrElse(false)
      }

    val nullableSymbols = flowCache.symbols.get(symbol) match {
      case Some(FunctionEdge(_, _, innerSymbols)) =>
        primitiveNullableSearch(innerSymbols)
      case Some(ValVarEdge(innerSymbols)) =>
        primitiveNullableSearch(innerSymbols)
    }

    val proofs = List(
      TagProof.PatchProof.fromPatch(constPatch),
      TagProof.SymbolsProof.fromSymbols(nullableSymbols)
    ).flatten

    if (proofs.nonEmpty) TagProp(Tag.Nullable, cond = true, proofs)
    else TagProp(Tag.Nullable, cond = false, List(TagProof.ContraryProof))
  }
}

object NullableInferrer {
  val constNullableSymbols = List(
    "java/io/File.getParent"
  ) // TODO possible can be constructed via Java reflection or bytecode analysis
}
