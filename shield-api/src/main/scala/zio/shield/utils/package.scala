package zio.shield

import scalafix.v1._

import scala.meta._
import scala.util.Try

package object utils {
  def selectNamesFromPat(p: Pat): List[Term.Name] = p match {
    case Pat.Var(name) => List(name)
    case Pat.Bind(lhs, rhs) =>
      selectNamesFromPat(lhs) ++ selectNamesFromPat(rhs)
    case Pat.Tuple(args)      => args.flatMap(selectNamesFromPat)
    case Pat.Extract(_, args) => args.flatMap(selectNamesFromPat)
    case Pat.ExtractInfix(lhs, _, rhs) =>
      selectNamesFromPat(lhs) ++ rhs.flatMap(selectNamesFromPat)
    case Pat.Typed(lhs, _) => selectNamesFromPat(lhs)
    case _                 => List.empty
  }

  implicit class SymbolInformationOps(symbolInfo: SymbolInformation) {
    def safeSignature: Option[Signature] =
      Try {
        symbolInfo.signature
      }.toOption
  }
}
