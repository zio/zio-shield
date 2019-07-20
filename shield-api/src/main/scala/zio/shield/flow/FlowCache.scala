package zio.shield.flow

import java.nio.file.Path

import scala.meta._
import scalafix.v1._
import zio.shield.tag._

import scala.collection.mutable

// TODO maybe we don't need buffers here
final case class FlowCache(
    files: mutable.Map[String, Path],
    docs: mutable.Map[Path, SemanticDocument],
    trees: mutable.Map[String, Tree],
    symbols: mutable.Map[String, FlowEdge], // edges between symbols
    userTags: mutable.Map[String, mutable.Buffer[TagProp[_]]] // tags provided by user via annotations
) {

  private[flow] val inferredTags
    : mutable.Map[String, mutable.Buffer[TagProp[_]]] =
    mutable.Map.empty

  def clear(): Unit = {
    files.clear()
    symbols.clear()
    userTags.clear()

    inferredTags.clear()
  }

  def build(semDocs: Map[Path, SemanticDocument]): Unit = {
    clear()

    semDocs.foreach {
      case (file, semDoc) =>
        def updateForSymbol(symbol: Symbol, tree: Tree, edge: FlowEdge): Unit =
          if (symbol.isGlobal) {
            files(symbol.value) = file
            docs(symbol.value) = semDoc
            trees(symbol.value) = tree
            userTags(symbol.value) = symbol
              .info(semDoc)
              .map(_.annotations)
              .getOrElse(List.empty)
              .collect {
                case Annotation(TypeRef(_, s: Symbol, _)) =>
                  TagProp.fromAnnotationSymbol(s.value)
              }
              .flatten
              .toBuffer
            symbols(symbol.value) = edge
          }

        def selectSymbolsFromPat(p: Pat): List[Symbol] = p match {
          case Pat.Var(name) => List(name.symbol(semDoc))
          case Pat.Bind(lhs, rhs) =>
            selectSymbolsFromPat(lhs) ++ selectSymbolsFromPat(rhs)
          case Pat.Tuple(args)      => args.flatMap(selectSymbolsFromPat)
          case Pat.Extract(_, args) => args.flatMap(selectSymbolsFromPat)
          case Pat.ExtractInfix(lhs, _, rhs) =>
            selectSymbolsFromPat(lhs) ++ rhs.flatMap(selectSymbolsFromPat)
          case Pat.Typed(lhs, _) => selectSymbolsFromPat(lhs)
          case _                 => List.empty
        }

        val traverser = new Traverser {
          override def apply(tree: Tree): Unit = tree match {
            case d: Defn.Def =>
              updateForSymbol(d.name.symbol(semDoc),
                              d,
                              FunctionEdge.fromDefn(d)(semDoc))
            case d: Defn.Class =>
              updateForSymbol(d.name.symbol(semDoc),
                              d,
                              ClassTraitEdge.fromDefn(d)(semDoc))
            case d: Defn.Trait =>
              updateForSymbol(d.name.symbol(semDoc),
                              d,
                              ClassTraitEdge.fromDefn(d)(semDoc))
            case d: Defn.Object =>
              updateForSymbol(d.name.symbol(semDoc),
                              d,
                              ObjectEdge.fromDefn(d)(semDoc))
            case d: Defn.Val =>
              d.pats.flatMap(selectSymbolsFromPat).foreach { s =>
                updateForSymbol(s, d, ValVarEdge.fromDefn(d)(semDoc))
              }
            case d: Defn.Var =>
              d.pats.flatMap(selectSymbolsFromPat).foreach { s =>
                updateForSymbol(s, d, ValVarEdge.fromDefn(d)(semDoc))
              }
          }
        }
    }
  }
}

class FlowCacheTagCheckerImpl(flowCache: FlowCache) extends TagChecker {
  def check(symbol: String, tag: Tag): Option[Boolean] = {
    val userTagsSymbol =
      flowCache.userTags.getOrElse(symbol, mutable.Buffer.empty)
    userTagsSymbol.find(_.tag == tag) match {
      case Some(cond) => Some(cond.cond)
      case None =>
        val inferredTagsSymbol =
          flowCache.inferredTags.getOrElse(symbol, mutable.Buffer.empty)
        inferredTagsSymbol.find(_.tag == tag) match {
          case Some(cond) => Some(cond.cond)
          case None       => None
        }
    }
  }
}

object FlowCache {
  def empty: FlowCache =
    FlowCache(mutable.Map.empty,
              mutable.Map.empty,
              mutable.Map.empty,
              mutable.Map.empty,
              mutable.Map.empty)
}
