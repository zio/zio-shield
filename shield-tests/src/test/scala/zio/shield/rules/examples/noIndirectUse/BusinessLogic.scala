package zio.shield.rules.examples.noIndirectUse

import zio.shield.rules.examples.noIndirectUse.Implementations.FooImpl
import zio.shield.rules.examples.noIndirectUse.PureInterfaces.Foo
import zio.{ Task, UIO, ZIO }

object BusinessLogic {
  def logic(foo: Foo): UIO[Unit] = foo.bar

  class MyBusinessLogic(foo: Foo) {
    def bar: UIO[Unit] = foo.bar
  }

  val doBar: ZIO[Foo, Nothing, Unit] = ZIO.accessM(_.bar)

  class Bar extends FooImpl {
    val bar2 = bar
  }

  trait Bar2
  class BarImpl extends FooImpl with Bar2 {
    val bar2 = bar
  }

  def logicBad(foo: Foo): Task[Unit] = foo.bar *> ZIO.effect(println("Hello"))

  class MyBusinessLogicBad(foo: FooImpl) {
    def bar: UIO[Unit] = foo.bar
  }

  val doBarBad: ZIO[FooImpl, Nothing, Unit] = ZIO.accessM(_.bar)
}
