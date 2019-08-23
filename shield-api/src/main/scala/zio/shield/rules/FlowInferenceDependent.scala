package zio.shield.rules

import zio.shield.flow.FlowInferrer

trait FlowInferenceDependent {
  def dependsOn: List[FlowInferrer[_]]
}
