package com.vovapolu.shield.rules.examples

import zio.Task

object ZioShieldNoPartialFunctionsExample {
  Option("abc").get
  List().head
  Left(123).right.get

  Task.effect {
    Option("abc").get
    List().head
    Left(123).right.get
  }
}
