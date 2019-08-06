package zio.shield.rules.examples.noIndirectUse

import zio.shield.rules.examples.noIndirectUse.Implementations.FooImpl

object Junk {
  val foo = new FooImpl

  foo.bar // indirect use!
}
