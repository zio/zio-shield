package zio.shield.tag

trait TagChecker {
  def check(symbol: String, tag: Tag): Option[Boolean]
}

object TagChecker {
  val empty: TagChecker = (symbol: String, tag: Tag) => None
}
