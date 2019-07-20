package zio.shield.rules

import scala.meta._
import scalafix.v1._

object ZioShieldNoTypeCasting extends SemanticRule("ZioShieldNoTypeCasting") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    val typeCastingSymbols = List(
      "scala/Any#asInstanceOf().",
      "scala/Any#isInstanceOf()."
    )

    def checkCase(cs: Case): Boolean = cs.pat match {
      case Pat.Typed(_, _) => true
      case _               => false
    }

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintSymbols(typeCastingSymbols) {
        case _ => "type casting"
      } orElse {
        case c: Case if checkCase(c) =>
          Patch.lint(Diagnostic("", "pattern match against type", c.pat.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
