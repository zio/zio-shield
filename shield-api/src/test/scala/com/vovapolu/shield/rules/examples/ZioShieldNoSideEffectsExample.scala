package com.vovapolu.shield.rules.examples

import zio.Task

object ZioShieldNoSideEffectsExample {
  def boom(): Unit = {
    println("boom")
  }

  println("abc")
  println()
  boom()
  boom
  List(1, 2, 3).foreach(i => ())

  Task.effect {
    println("abc")
    println()
    boom()
    boom
    List(1, 2, 3).foreach(i => ())
  }
}
