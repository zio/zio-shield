package zio.shield.rules.examples.noIndirectUse

import zio.UIO

object PureInterfaces {
  trait Foo {
    def bar: UIO[Unit]
  }
}
