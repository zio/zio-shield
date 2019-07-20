package zio.shield.rules.examples

object Playground2 {
  type UIO[T] = T
  object UIO {
    def apply(unit: Unit): Any = ???
  }

  trait Foo {
    def bar: UIO[Unit]
  }

  class FooImpl extends Foo {
    def bar: UIO[Unit] = UIO(println("Hi"))
  }
}
