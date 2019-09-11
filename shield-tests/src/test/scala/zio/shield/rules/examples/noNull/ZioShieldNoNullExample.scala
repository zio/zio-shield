package zio.shield.rules.examples.noNull

import java.io.File

import zio.Task

object ZioShieldNoNullExample {
  null
  new File(".").getParent
  ZioShieldNoNullExample2.annotatedNullable("foo")
  ZioShieldNoNullExample2.defBodyNullable("foo")
  ZioShieldNoNullExample2.defBodyNonNullable("foo")
  ZioShieldNoNullExample2.annotatedNonNullable("foo")
  ZioShieldNoNullExample2.valBodyNullable
  ZioShieldNoNullExample2.usingNullable("foo")

  Task.effect {
    null
    new File(".").getParent
    ZioShieldNoNullExample2.annotatedNullable("foo")
    ZioShieldNoNullExample2.defBodyNullable("foo")
    ZioShieldNoNullExample2.defBodyNonNullable("foo")
    ZioShieldNoNullExample2.annotatedNonNullable("foo")
    ZioShieldNoNullExample2.valBodyNullable
    ZioShieldNoNullExample2.usingNullable("foo")
  }
}
