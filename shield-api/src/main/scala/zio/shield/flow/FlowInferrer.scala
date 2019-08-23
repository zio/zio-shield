package zio.shield.flow

import zio.shield.tag._

trait FlowInferrer[T <: Tag] {
  def name: String
  def isInferable(symbol: String, edge: FlowEdge): Boolean
  def dependentSymbols(edge: FlowEdge): List[String]
  def infer(flowCache: FlowCache)(symbol: String): TagProp[T]
}
