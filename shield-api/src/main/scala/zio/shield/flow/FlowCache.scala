package zio.shield.flow

import java.nio.file.Path

import scala.meta._
import scalafix.v1._
import zio.shield.tag._
import zio.shield.utils

import scala.collection.mutable

trait FlowCache {
  def files: mutable.Map[String, Path]
  def docs: mutable.Map[Path, SemanticDocument]
  def trees: mutable.Map[String, Tree]
  def edges: mutable.Map[String, FlowEdge]

  def userTags: mutable.Map[String, mutable.Buffer[TagProp[_]]]
  def inferredTags: mutable.Map[String, mutable.Buffer[TagProp[_]]]

  def clear(): Unit
  def build(semDocs: Map[Path, SemanticDocument]): Unit
  def searchTag(tag: Tag)(symbol: String): Option[Boolean]
  def infer(inferrers: List[FlowInferrer[_]]): Unit
}

// TODO maybe we don't need buffers here
final case class FlowCacheImpl(
    files: mutable.Map[String, Path],
    docs: mutable.Map[Path, SemanticDocument],
    trees: mutable.Map[String, Tree],
    edges: mutable.Map[String, FlowEdge], // edges between symbols
    userTags: mutable.Map[String, mutable.Buffer[TagProp[_]]] // tags provided by user via annotations
) extends FlowCache {

  val inferredTags: mutable.Map[String, mutable.Buffer[TagProp[_]]] =
    mutable.Map.empty

  def searchTag(tag: Tag)(symbol: String): Option[Boolean] = {
    def findProp(tags: mutable.Map[String, mutable.Buffer[TagProp[_]]])
      : Option[Boolean] =
      tags
        .getOrElse(symbol, mutable.Buffer.empty)
        .find(p => p.tag == tag && p.isProved)
        .map(_.cond)

    lazy val userProp = findProp(userTags)

    lazy val inferredProp = findProp(inferredTags)

    userProp.orElse(inferredProp)
  }

  def clear(): Unit = {
    files.clear()
    edges.clear()
    userTags.clear()

    inferredTags.clear()
  }

  def build(semDocs: Map[Path, SemanticDocument]): Unit = {
    clear()

    semDocs.foreach {
      case (file, semDoc) =>
        implicit val doc: SemanticDocument = semDoc

        def updateForSymbol(symbol: Symbol,
                            tree: Tree,
                            edge: FlowEdge): Unit = {
          if (symbol.isGlobal) {
            files(symbol.value) = file
            docs(file) = semDoc
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
            edges(symbol.value) = edge
          }
        }

        val traverser = new Traverser {
          override def apply(tree: Tree): Unit = tree match {
            case d: Defn.Def =>
              updateForSymbol(d.name.symbol, d, FunctionEdge.fromDefn(d))
              super.apply(tree)
            case d: Decl.Def =>
              updateForSymbol(d.name.symbol, d, FunctionEdge.fromDecl(d))
            case d: Defn.Class =>
              updateForSymbol(d.name.symbol, d, ClassTraitEdge.fromDefn(d))
              super.apply(tree)
            case d: Defn.Trait =>
              updateForSymbol(d.name.symbol, d, ClassTraitEdge.fromDefn(d))
              super.apply(tree)
            case d: Defn.Object =>
              updateForSymbol(d.name.symbol, d, ObjectEdge.fromDefn(d))
              super.apply(tree)
            case d: Defn.Val =>
              d.pats.flatMap(utils.selectNamesFromPat).foreach { name =>
                updateForSymbol(name.symbol, d, ValVarEdge.fromDefn(d))
              }
              super.apply(tree)
            case d: Defn.Var =>
              d.pats.flatMap(utils.selectNamesFromPat).foreach { name =>
                updateForSymbol(name.symbol, d, ValVarEdge.fromDefn(d))
              }
              super.apply(tree)
            case d: Decl.Val =>
              d.pats.flatMap(utils.selectNamesFromPat).foreach { name =>
                updateForSymbol(name.symbol, d, ValVarEdge.empty)
              }
            case d: Decl.Var =>
              d.pats.flatMap(utils.selectNamesFromPat).foreach { name =>
                updateForSymbol(name.symbol, d, ValVarEdge.empty)
              }
            case _ => super.apply(tree)
          }
        }

        traverser(semDoc.tree)
    }
  }

  def infer(inferrers: List[FlowInferrer[_]]): Unit = if (inferrers.nonEmpty) {
    val processingSymbols = mutable.HashSet[String]()

    def dfs(symbol: String): Unit = {
      if (!processingSymbols.contains(symbol) &&
          (!inferredTags.contains(symbol) || inferredTags(symbol).isEmpty)) {
        processingSymbols += symbol
        edges.get(symbol).foreach { e =>
          inferrers.foreach { i =>
            if (i.isInferable(symbol, e)) {
              i.dependentSymbols(e).foreach(dfs)
            }
          }
          if (!inferredTags.contains(symbol))
            inferredTags(symbol) = mutable.Buffer()
          inferrers.foreach { i =>
            if (i.isInferable(symbol, e)) {
              inferredTags(symbol) += i.infer(this)(symbol)
            }
          }
        }
      }
    }

    edges.keys.foreach(dfs)
  }
}

object FlowCache {
  def empty: FlowCache =
    FlowCacheImpl(mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty)
}
