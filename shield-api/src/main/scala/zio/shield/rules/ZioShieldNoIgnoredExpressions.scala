package zio.shield.rules

import scalafix.v1._

import scala.meta._

object ZioShieldNoIgnoredExpressions
    extends SemanticRule("ZioShieldNoIgnoredExpressions") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    def findIgnoredExpressions(exprs: List[Stat],
                               skipLast: Boolean = false): List[Stat] = {
      val possiblyIgnored = if (skipLast) exprs.dropRight(1) else exprs

      possiblyIgnored.collect {
        case t: Term if !ZioBlockDetector.safeBlocksMatcher.matches(t.symbol) => t
      }
    }

    object IgnoredExpressions {
      private def wrapNonEmptyList(l: List[Stat]): Option[List[Stat]] =
        if (l.nonEmpty) Some(l) else None

      def unapply(t: Tree): Option[List[Stat]] = t match {
        case d: Defn.Class =>
          wrapNonEmptyList(findIgnoredExpressions(d.templ.stats))
        case d: Defn.Object =>
          wrapNonEmptyList(findIgnoredExpressions(d.templ.stats))
        case d: Defn.Trait =>
          wrapNonEmptyList(findIgnoredExpressions(d.templ.stats))
        case b: Term.Block =>
          wrapNonEmptyList(findIgnoredExpressions(b.stats, skipLast = true))
        case _ => None
      }
    }

    ZioBlockDetector {
      case IgnoredExpressions(exprs) =>
        exprs.map(e => Patch.lint(Diagnostic("", "ignored expression", e.pos)))
    }.traverse(doc.tree)
  }
}
