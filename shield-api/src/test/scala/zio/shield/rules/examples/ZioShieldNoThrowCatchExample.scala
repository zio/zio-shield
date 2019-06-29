package zio.shield.rules.examples

import zio.Task

object ZioShieldNoThrowCatchExample {
  try {
    List().head
  } catch {
    case e: Exception =>
  }
  throw new RuntimeException("boom!")

  Task.effect {
    try {
      List().head
    } catch {
      case e: Exception =>
    }
    throw new RuntimeException("boom!")
  }
}
