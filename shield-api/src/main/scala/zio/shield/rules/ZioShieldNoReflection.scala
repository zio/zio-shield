package zio.shield.rules

import scalafix.v1._
import scala.meta._

object ZioShieldNoReflection extends SemanticRule("ZioShieldNoReflection") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintPrefix("scala/reflect") {
        case _ => "scala reflection"
      } orElse
        ZioBlockDetector.lintPrefixes(
          List("java/lang/reflect", "java/lang/Class")) {
          case _ => "java reflection"
        }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree, ignoreInZioBlocks = false)
  }
}
