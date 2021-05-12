package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.{ FlowCache, PartialityInferrer }
import zio.shield.tag.Tag

import scala.meta._

object ZioShieldNoPartial extends FlowRule {
  val name = "ZioShieldNoPartial"

  val dependsOn = List(PartialityInferrer)

  def fix(cache: FlowCache)(implicit doc: SemanticDocument): Patch = {

    val pf: PartialFunction[Tree, Patch] =
      ZioBlockDetector.lintFunction(s => cache.searchTag(Tag.Partial)(s.value).getOrElse(false)) {
        case _ => "possible partial symbol" // TODO print proof
      } orElse {
        case l: Term.Throw =>
          Patch.lint(Diagnostic("", "not total: throwing exceptions", l.pos))
        case l: Term.Try =>
          Patch.lint(Diagnostic("", "not total: try/catch block", l.pos))
      }

    ZioBlockDetector.fromSingleLintPerTree(pf).traverse(doc.tree)
  }
}
