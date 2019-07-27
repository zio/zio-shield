package zio.shield.tag

sealed trait Tag

object Tag {

  // See https://github.com/vovapolu/zio-shield/issues/5#issuecomment-506896092
  case object PureInterface extends Tag

  case object Implementation extends Tag

  case object BusinessLogic extends Tag

  // See https://github.com/vovapolu/zio-shield/issues/7#issuecomment-508237712
  case object Impure extends Tag

  case object Partial extends Tag

  case object Nullable extends Tag
}
