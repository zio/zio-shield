package zio.shield.rules

import scalafix.v1._

import scala.collection.mutable
import scala.meta._

import zio.shield.utils.SymbolInformationOps

object ZioShieldNoFutureMethods
    extends SemanticRule("ZioShieldNoFutureMethods") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    def getType(symbol: Symbol): Option[SemanticType] =
      symbol.info.flatMap(_.safeSignature).collect {
        case MethodSignature(_, _, returnType) => returnType
      }

    def detectFutureType(tpe: SemanticType): Boolean = tpe match {
      case TypeRef(_, s, _) if s.value == "scala/concurrent/Future#" => true
      case _                                                         => false
    }

    val traverser = new Traverser {
      val lints = mutable.Buffer[Patch]()

      override def apply(tree: Tree): Unit = tree match {
        case Defn.Val(_, List(Pat.Var(name)), _, _)
            if getType(name.symbol).exists(detectFutureType) =>
          lints += Patch.lint(
            Diagnostic("", "Future returning method", name.pos))
        case Defn.Def(_, name, _, _, _, _)
            if getType(name.symbol).exists(detectFutureType) =>
          lints += Patch.lint(
            Diagnostic("", "Future returning method", name.pos))
        case _ => super.apply(tree)
      }
    }

    traverser(doc.tree)
    traverser.lints.asPatch
  }
}
