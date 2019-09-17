package zio.shield.flow

import java.nio.file.Path

import scala.meta._
import scalafix.v1._
import zio.shield.tag._
import zio.shield.utils

import scala.collection.mutable

trait FlowCache {
  def files: mutable.Map[String, Path] // TODO add inverse index for files
  def docs: mutable.Map[Path, SemanticDocument]
  def trees: mutable.Map[String, Tree]
  def edges: mutable.Map[String, FlowEdge]

  def userTags: mutable.Map[String, mutable.Buffer[TagProp[_]]]
  def inferredTags: mutable.Map[String, mutable.Buffer[TagProp[_]]]

  def clear(): Unit
  def update(semDocs: Map[Path, SemanticDocument]): Unit

  def searchTag(tag: Tag)(symbol: String): Option[Boolean]

  def deepInferAndCache(inferrers: List[FlowInferrer[_]])(
      files: List[Path]): Unit

  def stats: FlowCache.Stats
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

  def update(semDocs: Map[Path, SemanticDocument]): Unit = {
    semDocs.foreach {
      case (file, semDoc) =>
        docs(file) = semDoc

        implicit val doc: SemanticDocument = semDoc

        def updateForSymbol(symbol: Symbol,
                            tree: Tree,
                            edge: FlowEdge): Unit = {
          if (symbol.isGlobal) {
            files(symbol.value) = file
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

  def deepInferAndCache(inferrers: List[FlowInferrer[_]])(
      targetFiles: List[Path]): Unit =
    if (inferrers.nonEmpty) {
      val processingSymbols = mutable.HashSet[String]()

      def dfs(symbol: String): Unit = {
        if (!processingSymbols.contains(symbol)) {
          processingSymbols += symbol
          edges.get(symbol).foreach { e =>
            inferrers.foreach { i =>
              if (i.isInferable(symbol, e)) {
                i.dependentSymbols(e).foreach(dfs)
              }
            }
            var inferredTagsBuffer: mutable.Buffer[TagProp[_]] = null
            if (!inferredTags.contains(symbol)) {
              inferredTagsBuffer = mutable.Buffer()
              inferredTags(symbol) = inferredTagsBuffer
            } else {
              inferredTagsBuffer = inferredTags(symbol)
              inferredTagsBuffer.clear()
            }
            inferrers.foreach { i =>
              if (i.isInferable(symbol, e)) {
                inferredTagsBuffer += i.infer(this)(symbol)
              }
            }
          }
        }
      }

      val targetFilesSet = targetFiles.toSet

      files.foreach {
        case (symbol, file) if targetFilesSet.contains(file) => dfs(symbol)
        case _ =>
      }
    }

  def stats: FlowCache.Stats =
    FlowCache.Stats(
      docs.size,
      edges.size,
      edges.values.map {
        case FunctionEdge(argsTypes, returnType, innerSymbols) =>
          argsTypes.size + returnType.size + innerSymbols.size
        case ClassTraitEdge(ctorArgsTypes, parentsTypes, innerDefns) =>
          ctorArgsTypes.size + parentsTypes.size + innerDefns.size
        case ObjectEdge(innerDefns)   => innerDefns.size
        case ValVarEdge(innerSymbols) => innerSymbols.size
      }.sum
    )
}

object FlowCache {
  def empty: FlowCache =
    FlowCacheImpl(mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty,
                  mutable.Map.empty)

  final case class Stats(filesCount: Int, symbolsCount: Int, edgesCount: Int)
}
