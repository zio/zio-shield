package example

object ExampleTest {
  def defBodyNullable(foo: String): String = {
    if (foo.length > 1) {
      foo
    } else {
      null
    }
  }

  defBodyNullable("foo")

  1.asInstanceOf[Long]
}
