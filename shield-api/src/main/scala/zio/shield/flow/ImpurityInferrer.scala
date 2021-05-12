package zio.shield.flow

import scalafix.v1._
import zio.shield.rules.ZioBlockDetector
import zio.shield.tag._
import zio.shield.utils.SymbolInformationOps

import scala.io.{ Source => ScalaSource }
import scala.meta._

case object ImpurityInferrer extends FlowInferrer[Tag.Impure.type] {

  val constImpureMatcher: SymbolMatcher = SymbolMatcher.normalized(
    ScalaSource
      .fromInputStream(getClass.getClassLoader.getResourceAsStream("impure_methods.txt"))
      .getLines()
      .toList: _*
  )

  def constImpurityChecker(implicit doc: SemanticDocument): PartialFunction[Tree, Patch] = {

    def isUnitMethod(s: Symbol): Boolean =
      s.info
        .flatMap(_.safeSignature)
        .collect {
          case MethodSignature(_, _, TypeRef(_, s: Symbol, List())) if s.value == "scala/Unit#" =>
            true
        }
        .getOrElse(false)

    ZioBlockDetector.lintFunction(isUnitMethod) {
      case _ => "impure: calling unit method"
    }
  }

  val name: String = toString

  def infer(flowCache: FlowCache)(symbol: String): TagProp[Tag.Impure.type] =
    if (ImpurityInferrer.constImpureMatcher.matches(Symbol(symbol))) {
      TagProp(Tag.Impure, cond = true, List(TagProof.GivenProof))
    } else {

//      val constPatch = flowCache.trees.get(symbol) match {
//        case Some(tree) =>
//          val maybePatch = for {
//            path <- flowCache.files.get(symbol)
//            doc <- flowCache.docs.get(path)
//            patch = tree.collect(constImpurityChecker(doc)).asPatch
//          } yield patch
//
//          maybePatch.getOrElse(Patch.empty)
//        case None => Patch.empty
//      }

      val constPatch = Patch.empty

      val impureSymbols = flowCache.edges.get(symbol) match {
        case Some(FunctionEdge(_, _, innerSymbols)) =>
          innerSymbols.filter(flowCache.searchTag(Tag.Impure)(_).getOrElse(false))
        case Some(ValVarEdge(innerSymbols)) =>
          innerSymbols.filter(flowCache.searchTag(Tag.Impure)(_).getOrElse(false))
        case _ => List.empty
      }

      val proofs = List(
        TagProof.PatchProof.fromPatch(constPatch),
        TagProof.SymbolsProof.fromSymbols(impureSymbols)
      ).flatten

      if (proofs.nonEmpty) TagProp(Tag.Impure, cond = true, proofs)
      else TagProp(Tag.Impure, cond = false, List(TagProof.ContraryProof))
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
