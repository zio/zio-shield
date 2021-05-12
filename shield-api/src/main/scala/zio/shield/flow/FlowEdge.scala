package zio.shield.flow

import scala.meta._
import scalafix.v1._

import scala.collection.mutable
import scala.meta.internal.semanticdb.Scala._

import zio.shield.utils.SymbolInformationOps

sealed trait FlowEdge

object FlowEdge {
  private[flow] def symbolsFromBody(body: Tree, skipSymbols: List[String] = List.empty)(
    implicit semDoc: SemanticDocument
  ): List[String] = {
    val innerSymbols = mutable.HashSet[String]()

    new Traverser {
      override def apply(tree: Tree): Unit = {
        val symbol = tree.symbol
        if (symbol.value.isGlobal && symbol.value.isTerm) {
          innerSymbols += symbol.value
        }
        if (!skipSymbols.contains(symbol.value)) {
          super.apply(tree)
        }
      }
    }.apply(body)

    innerSymbols.toList
  }
}

final case class FunctionEdge(argsTypes: List[String], returnType: Option[String], innerSymbols: List[String]) // only Terms
    extends FlowEdge

object FunctionEdge {
  private def fromParamsAndName(paramss: List[List[Term.Param]], name: Term.Name)(
    implicit doc: SemanticDocument
  ): (List[String], Option[String]) = {
    val argsTypes =
      paramss.flatten
        .flatMap(_.decltpe)
        .map(_.symbol)
        .filter(_.isGlobal)
        .map(_.value)
    val returnType = name.symbol.info.flatMap(_.safeSignature).collect {
      case MethodSignature(_, _, TypeRef(_, s: Symbol, _)) => s.value
    }

    (argsTypes, returnType)
  }

  def fromDecl(d: Decl.Def)(implicit semDoc: SemanticDocument): FunctionEdge = {
    val (argsTypes, returnType) = fromParamsAndName(d.paramss, d.name)

    FunctionEdge(argsTypes, returnType, List.empty)
  }

  def fromDefn(d: Defn.Def)(implicit semDoc: SemanticDocument): FunctionEdge = {
    val (argsTypes, returnType) = fromParamsAndName(d.paramss, d.name)
    val innerSymbols            = FlowEdge.symbolsFromBody(d.body)

    FunctionEdge(argsTypes, returnType, innerSymbols)
  }
}

final case class ClassTraitEdge(
  ctorArgsTypes: List[String],
  parentsTypes: List[String],
  innerDefns: List[String],
  innerSymbols: List[String]
) extends FlowEdge

object ClassTraitEdge {
  private def fromCtorAndName(ctor: Ctor.Primary, name: Type.Name)(
    implicit semDoc: SemanticDocument
  ): (List[String], List[String], List[String]) = {
    val ctorArgsTypes =
      ctor.paramss.flatten
        .flatMap(_.decltpe)
        .map(_.symbol) // TOOD we need more detailed analysis
        .filter(_.isGlobal)
        .map(_.value)
    val (parentTypes, innerDefns) = name.symbol.info
      .flatMap(_.safeSignature)
      .collect {
        case ClassSignature(_, parents, _, decls) =>
          val parentTypes = parents.collect {
            case TypeRef(_, s: Symbol, _) => s.value
          }
          val innerDefns = decls.collect {
            case info if info.symbol.isGlobal => info.symbol.value
          }
          (parentTypes, innerDefns)
      }
      .getOrElse((List.empty, List.empty))

    (ctorArgsTypes, parentTypes, innerDefns)
  }

  def fromDefn(d: Defn.Class)(implicit semDoc: SemanticDocument): ClassTraitEdge = {
    val (ctorArgsTypes, parentTypes, innerDefns) =
      fromCtorAndName(d.ctor, d.name)
    val innerSymbols = d.templ.stats
      .flatMap(FlowEdge.symbolsFromBody(_, skipSymbols = innerDefns))

    ClassTraitEdge(ctorArgsTypes, parentTypes, innerDefns, innerSymbols)
  }

  def fromDefn(d: Defn.Trait)(implicit semDoc: SemanticDocument): ClassTraitEdge = {
    val (ctorArgsTypes, parentTypes, innerDefns) =
      fromCtorAndName(d.ctor, d.name)
    ClassTraitEdge(ctorArgsTypes, parentTypes, innerDefns, List.empty)
  }
}

final case class ObjectEdge(innerDefns: List[String], innerSymbols: List[String]) extends FlowEdge

object ObjectEdge {
  def fromDefn(d: Defn.Object)(implicit semDoc: SemanticDocument): ObjectEdge = {
    val innerDefns = d.name.symbol.info
      .flatMap(_.safeSignature)
      .collect {
        case ClassSignature(_, _, _, decls) => decls.map(_.symbol.value)
      }
      .getOrElse(List.empty)
    val innerSymbols = d.templ.stats
      .flatMap(FlowEdge.symbolsFromBody(_, skipSymbols = innerDefns))

    ObjectEdge(innerDefns, innerSymbols)
  }
}

final case class ValVarEdge(innerSymbols: List[String]) extends FlowEdge // only Terms

object ValVarEdge {
  val empty = ValVarEdge(List.empty)

  private def fromBody(body: Term)(implicit semDoc: SemanticDocument): ValVarEdge = {
    val innerSymbols = mutable.HashSet[String]()

    new Traverser {
      override def apply(tree: Tree): Unit = tree match {
        case t =>
          val symbol = t.symbol
          if (symbol.value.isGlobal && symbol.value.isTerm) {
            innerSymbols += symbol.value
          }
      }
    }.apply(body)

    ValVarEdge(innerSymbols.toList)
  }

  def fromDefn(d: Defn.Val)(implicit semDoc: SemanticDocument): ValVarEdge =
    fromBody(d.rhs)
  def fromDefn(d: Defn.Var)(implicit semDoc: SemanticDocument): ValVarEdge =
    d.rhs.map(fromBody).getOrElse(ValVarEdge(List.empty))
}
