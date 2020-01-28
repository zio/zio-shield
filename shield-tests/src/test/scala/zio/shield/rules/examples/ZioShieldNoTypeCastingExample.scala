package zio.shield.rules.examples

import zio.UIO

object ZioShieldNoTypeCastingExample {
  1.asInstanceOf[Long]
  2.isInstanceOf[Long]
  (1: Any) match {
    case i: Int  => i.toLong
    case l: Long => l
  }

  UIO {
    1.asInstanceOf[Long]
    2.isInstanceOf[Long]
    (1: Any) match {
      case i: Int  => i.toLong
      case l: Long => l
    }
  }
}
