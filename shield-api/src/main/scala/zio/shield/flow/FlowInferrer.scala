package zio.shield.flow

import zio.shield.tag._

trait FlowInferrer[T <: Tag] {

  def flowCache: FlowCache

  def infer(symbol: String): TagProp[T]
}
