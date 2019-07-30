package zio.shield.rules

import scalafix.v1._
import zio.shield.flow._
import zio.shield.tag.Tag

import scala.meta._

class ZioShieldValidateImplementations(cache: FlowCache)
    extends SemanticRule("ZioShieldValidateImplementations") {

  override def fix(implicit doc: SemanticDocument): Patch = {

    def pureInterfaceParents(symbol: Symbol): List[String] =
      symbol.info match {
        case Some(info) =>
          info.signature match {
            case ClassSignature(_, parents, _, _) =>
              parents.collect {
                case TypeRef(_, s, _)
                    if cache
                      .searchTag(Tag.PureInterface)(s.value)
                      .getOrElse(false) =>
                  s.value
              }
            case _ => List.empty
          }
        case _ => List.empty
      }

    def fromBody(tree: Tree): Patch =
      tree.collect {
        case t if ZioBlockDetector.safeBlockDetector(t) =>
          Patch.lint(Diagnostic("", "effectful: ZIO effects usage outside of pure interface", t.pos))
      }.asPatch

    doc.tree.collect {
      // TODO currently there is no way to detect if the method overrides pure interface
      // we skip all the classes that extends pure interfaces, but that's wrong in some cases
      case d: Defn.Class if pureInterfaceParents(d.name.symbol).isEmpty =>
        d.templ.stats.map {
          case d: Defn.Def => fromBody(d.body)
          case d: Defn.Val => fromBody(d.rhs)
          case d: Defn.Var => d.rhs.fold(Patch.empty)(fromBody)
        }.asPatch
      case d: Defn.Trait if pureInterfaceParents(d.name.symbol).isEmpty =>
        d.templ.stats.map {
          case d: Defn.Def => fromBody(d.body)
          case d: Defn.Val => fromBody(d.rhs)
          case d: Defn.Var => d.rhs.fold(Patch.empty)(fromBody)
        }.asPatch
    }.asPatch
  }
}
