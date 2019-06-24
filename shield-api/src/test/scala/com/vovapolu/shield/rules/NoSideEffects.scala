package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoSideEffects extends ZioShieldTest(ZioShieldNoSideEffects) {
  val tests = Tests {
    test("example") {
      val List(lint1: Lint, lint2: Lint, lint3: Lint) = zioShieldInstance.run(srcPath)

      lint1.path ==> srcPath
      lint1.message ==> "possible side-effect"
      lint1.position.text ==> "println"

      lint2.path ==> srcPath
      lint2.message ==> "possible side-effect"
      lint2.position.text ==> "println"

      lint3.path ==> srcPath
      lint3.message ==> "possible side-effect"
      lint3.position.text ==> "println"
    }
  }
}
