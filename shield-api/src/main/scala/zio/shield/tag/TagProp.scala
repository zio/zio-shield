package zio.shield.tag

import scalafix.patch.Patch

import scala.annotation.StaticAnnotation

sealed trait TagProof {
  def isEmpty: Boolean
  def nonEmpty: Boolean = !isEmpty
}

object TagProof {

  final case class PatchProof(patch: Patch) extends TagProof {
    override def isEmpty: Boolean = patch.isEmpty
  }

  object PatchProof {
    def fromPatch(patch: Patch): Option[PatchProof] =
      if (patch.nonEmpty) Some(PatchProof(patch)) else None
  }

  final case class AnnotationProof(anot: StaticAnnotation) extends TagProof {
    override def isEmpty: Boolean = false
  }

  final case class SymbolsProof(symbols: List[String]) extends TagProof {
    override def isEmpty: Boolean = symbols.isEmpty
  }

  object SymbolsProof {
    def fromSymbols(symbols: List[String]): Option[SymbolsProof] =
      if (symbols.nonEmpty) Some(SymbolsProof(symbols)) else None
  }

  case object ContraryProof extends TagProof {
    override def isEmpty: Boolean = false
  }

  case object GivenProof extends TagProof {
    override def isEmpty: Boolean = false
  }
}

case class TagProp[T <: Tag](tag: T, cond: Boolean, proofs: List[TagProof]) {
  def isProved: Boolean = proofs.exists(_.nonEmpty)
}

object TagProp {

  def fromAnnotationSymbol(symbol: String): Option[TagProp[_]] =
    symbol match {
      case s =>
        println(s"annotation symbol: $s")
        None
    }
}
