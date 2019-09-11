package zio.shield.rules.examples.noPartial

import zio.shield.annotation._

object ZioShieldNoPartialFunctionsExample2 {
  @partial
  def annotatedPartial(foo: String): String = throw new RuntimeException()

  def defBodyPartial(foo: String): String = {
    if (foo.length > 1) {
      foo
    } else {
      throw new RuntimeException()
    }
  }

  def defBodyTotal(foo: String): String = foo

  @total
  def annotatedTotal(foo: String): String = throw new RuntimeException()

  val valBodyPartial: String = throw new RuntimeException()

  def usingPartial(foo: String): String = {
    if (foo.length > 1) {
      foo
    } else {
      defBodyPartial(foo)
    }
  }
}
