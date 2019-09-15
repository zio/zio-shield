package zio.shield.rules.examples.noIndirectUse

import zio.UIO
import zio.shield.rules.examples.noIndirectUse.PureInterfaces.Foo

object Implementations {
  class FooImpl extends Foo {
    def bar: UIO[Unit] = UIO(println("Hi"))
  }

  class FooImplBad {
    def bar: UIO[Unit] = UIO(println("Hi"))
  }
}
