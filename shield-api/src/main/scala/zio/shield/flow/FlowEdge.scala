package zio.shield.flow

import scala.meta._
import scalafix.v1._

import scala.collection.mutable

sealed trait FlowEdge

final case class FunctionEdge(argsTypes: List[String],
                              returnType: Option[String],
                              innerSymbols: List[String])
    extends FlowEdge

object FunctionEdge {
  def fromDefn(d: Defn.Def)(implicit semDoc: SemanticDocument): FunctionEdge = {
    val argsTypes =
      d.paramss.flatten
        .flatMap(_.decltpe)
        .map(_.symbol)
        .filter(_.isGlobal)
        .map(_.value)
    val returnType = d.name.symbol.info.flatMap(_.signature match {
      case MethodSignature(_, _, TypeRef(_, s: Symbol, _)) => Some(s.value)
      case _                                               => None
    })

    val innerSymbols = mutable.HashSet[String]()

    new Traverser {
      override def apply(tree: Tree): Unit = tree match {
        case t =>
          val symbol = t.symbol
          if (symbol.isGlobal) {
            innerSymbols += symbol.value
          }
      }
    }.apply(d.body)

    FunctionEdge(argsTypes, returnType, innerSymbols.toList)
  }
}

final case class ClassTraitEdge(ctorArgsTypes: List[String],
                                parentsTypes: List[String],
                                innerDefns: List[String])
    extends FlowEdge

object ClassTraitEdge {
  private def fromCtorAndName(ctor: Ctor.Primary, name: Type.Name)(
      implicit semDoc: SemanticDocument): ClassTraitEdge = {
    val ctorArgsTypes =
      ctor.paramss.flatten
        .flatMap(_.decltpe)
        .map(_.symbol)
        .filter(_.isGlobal)
        .map(_.value)
    val (parentTypes, innerDefns) = name.symbol.info
      .flatMap(_.signature match {
        case ClassSignature(_, parents, _, decls) =>
          val parentTypes = parents.collect {
            case TypeRef(_, s: Symbol, _) => s.value
          }
          val innerDefns = decls.collect {
            case info if info.symbol.isGlobal => info.symbol.value
          }
          Some((parentTypes, innerDefns))
        case _ => None
      })
      .getOrElse((List.empty, List.empty))

    ClassTraitEdge(ctorArgsTypes, parentTypes, innerDefns)
  }

  def fromDefn(d: Defn.Class)(
      implicit semDoc: SemanticDocument): ClassTraitEdge =
    fromCtorAndName(d.ctor, d.name)

  def fromDefn(d: Defn.Trait)(
      implicit semDoc: SemanticDocument): ClassTraitEdge =
    fromCtorAndName(d.ctor, d.name)
}

final case class ObjectEdge(innerDefns: List[String]) extends FlowEdge

object ObjectEdge {
  def fromDefn(d: Defn.Object)(
      implicit semDoc: SemanticDocument): ObjectEdge = {
    val innerDefns = d.name.symbol.info
      .flatMap(_.signature match {
        case ClassSignature(_, _, _, decls) => Some(decls.map(_.symbol.value))
        case _                              => None
      })
      .getOrElse(List.empty)

    ObjectEdge(innerDefns)
  }
}

final case class ValVarEdge(innerSymbols: List[String]) extends FlowEdge

object ValVarEdge {
  private def fromBody(body: Term)(
      implicit semDoc: SemanticDocument): ValVarEdge = {
    val innerSymbols = mutable.HashSet[String]()

    new Traverser {
      override def apply(tree: Tree): Unit = tree match {
        case t =>
          val symbol = t.symbol
          if (symbol.isGlobal) {
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
