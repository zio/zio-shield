package zio.shield.rules.examples

import scala.reflect.runtime.universe

object ZioShieldNoReflectionExample {
  class Foo {}
  object Foo

  universe.typeOf[Foo].companion

  val constr = new Foo().getClass.getConstructor()
}
