package zio.shield.rules

import scalafix.v1._
import zio.shield.flow.{ FlowCache, FlowInferrer }

trait FlowRule { flowRule =>
  def name: String
  def dependsOn: List[FlowInferrer[_]]
  def fix(flowCache: FlowCache)(implicit doc: SemanticDocument): Patch
  def toRule(flowCache: FlowCache): Rule = new SemanticRule(name) {
    override def fix(implicit doc: SemanticDocument): Patch =
      flowRule.fix(flowCache)
  }
}
