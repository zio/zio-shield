package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.{ FlowCache, NullabilityInferrer }
import zio.shield.tag.Tag

import scala.meta._

object ZioShieldNoNull extends FlowRule {
  val name = "ZioShieldNoNull"

  val dependsOn = List(NullabilityInferrer)

  def fix(cache: FlowCache)(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s => cache.searchTag(Tag.Nullable)(s.value).getOrElse(false)) {
        case _ => "possibly nullable" // TODO print proof
      } orElse {
        case l: Lit.Null =>
          Patch.lint(Diagnostic("", "nullable: null usage", l.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
