package com.vovapolu.shield.rules

import scalafix.v1._

import scala.collection.mutable
import scala.meta._
import scala.util.Try

object ZioShieldNoFutureMethods
    extends SemanticRule("ZioShieldNoFutureMethods") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    def getType(symbol: Symbol): Option[SemanticType] = {
      println(symbol)
      println(Try(symbol.info.get.signature))
      Try(symbol.info.get.signature).toOption match {
        case Some(MethodSignature(_, _, returnType)) =>
          Some(returnType)
        case _ => None
      }
    }

    def detectFutureType(tpe: SemanticType): Boolean = {
      println(tpe)
      false
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
