package zio.shield.rules.examples.noNull

import zio.shield.Annotation._

object ZioShieldNoNullExample2 {

  @nullable
  def annotatedNullable(foo: String): String = null

  def defBodyNullable(foo: String): String =
    if (foo.length > 1) {
      foo
    } else {
      null
    }

  def defBodyNonNullable(foo: String): String = foo

  @nonNullable
  def annotatedNonNullable(foo: String): String = null

  val valBodyNullable = null

  def usingNullable(foo: String): String =
    if (foo.length > 1) {
      foo
    } else {
      defBodyNullable(foo)
    }
}
