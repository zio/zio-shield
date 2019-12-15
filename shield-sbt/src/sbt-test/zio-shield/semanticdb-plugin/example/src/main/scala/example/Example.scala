package example

object Example {
  def defBodyNullable(foo: String): String = {
    if (foo.length > 1) {
      foo
    } else {
      null
    }
  }

  def nullable(foo: String): String = null

  def safeF(bar: String): String = nullable(bar)

  defBodyNullable("foo")

  1.asInstanceOf[Long]
}
