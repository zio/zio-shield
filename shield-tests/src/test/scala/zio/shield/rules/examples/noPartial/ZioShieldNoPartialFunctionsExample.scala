package zio.shield.rules.examples.noPartial

import java.io.File

import zio.Task

object ZioShieldNoPartialFunctionsExample {
  Option("abc").get
  List().head
  Left(123).right.get
  new File("foo.bar").createNewFile()
  1.wait()
  ZioShieldNoPartialFunctionsExample2.annotatedPartial("foo")
  ZioShieldNoPartialFunctionsExample2.defBodyPartial("foo")
  ZioShieldNoPartialFunctionsExample2.defBodyTotal("foo")
  ZioShieldNoPartialFunctionsExample2.annotatedTotal("foo")
  ZioShieldNoPartialFunctionsExample2.valBodyPartial
  ZioShieldNoPartialFunctionsExample2.usingPartial("foo")

  Task.effect {
    Option("abc").get
    List().head
    Left(123).right.get
    new File("foo.bar").createNewFile()
    1.wait()
    ZioShieldNoPartialFunctionsExample2.annotatedPartial("foo")
    ZioShieldNoPartialFunctionsExample2.defBodyPartial("foo")
    ZioShieldNoPartialFunctionsExample2.defBodyTotal("foo")
    ZioShieldNoPartialFunctionsExample2.annotatedTotal("foo")
    ZioShieldNoPartialFunctionsExample2.valBodyPartial
    ZioShieldNoPartialFunctionsExample2.usingPartial("foo")
  }
}
