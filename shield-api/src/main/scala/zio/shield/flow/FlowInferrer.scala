package zio.shield.flow

import zio.shield.tag._

trait FlowInferrer[T <: Tag] {
  def infer(flowCache: FlowCache)(symbol: String): TagProp[T]
}
