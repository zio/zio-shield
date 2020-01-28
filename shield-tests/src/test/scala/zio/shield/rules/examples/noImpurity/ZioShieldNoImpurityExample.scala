package zio.shield.rules.examples.noImpurity

import zio.Task

object ZioShieldNoImpurityExample {
  def boom(): Unit =
    println("boom")

  println("abc")
  println()
  boom()
  boom
  List(1, 2, 3).foreach(i => ())
  ZioShieldNoImpurityExample2.annotatedImpure("foo")
  ZioShieldNoImpurityExample2.defBodyImpure("foo")
  ZioShieldNoImpurityExample2.defBodyPure("foo")
  ZioShieldNoImpurityExample2.annotatedPure("foo")
  ZioShieldNoImpurityExample2.valBodyImpure
  ZioShieldNoImpurityExample2.usingImpure("foo")

  Task.effect {
    println("abc")
    println()
    boom()
    boom
    List(1, 2, 3).foreach(i => ())
    ZioShieldNoImpurityExample2.annotatedImpure("foo")
    ZioShieldNoImpurityExample2.defBodyImpure("foo")
    ZioShieldNoImpurityExample2.defBodyPure("foo")
    ZioShieldNoImpurityExample2.annotatedPure("foo")
    ZioShieldNoImpurityExample2.valBodyImpure
    ZioShieldNoImpurityExample2.usingImpure("foo")
  }
}
