package zio.shield.rules.examples.noImpurity

import zio.shield.annotation.{impure, pure}

object ZioShieldNoImpurityExample2 {
  @impure
  def annotatedImpure(foo: String): Unit = println(foo)

  def defBodyImpure(foo: String): Unit = {
    if (foo.length > 1) {
      println(foo)
    } else {
      println("boom")
    }
  }

  def defBodyPure(foo: String): String = foo

  @pure
  def annotatedPure(foo: String): Unit = println(foo)

  val valBodyImpure = println("boom")

  def usingImpure(foo: String): String = {
    if (foo.length > 1) {
      foo
    } else {
      defBodyImpure(foo)
      foo
    }
  }
}
