package com.vovapolu.shield.rules.examples

import java.io.File

import zio.Task

object ZioShieldNoNullExample {
  null
  new File(".").getParent

  Task.effect {
    null
    new File(".").getParent
  }
}
