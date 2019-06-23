package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoPartialFunctionsTest
    extends ZioShieldTest(ZioShieldNoPartialFunctions) {
  val tests = Tests {
    test("example") {
      val List(lint1: Lint, lint2: Lint, lint3: Lint) = zioShieldInstance.run(srcPath)

      lint1.path ==> srcPath
      lint1.message ==> "not a total function"
      lint1.position.text ==> "get"

      lint2.path ==> srcPath
      lint2.message ==> "not a total function"
      lint2.position.text ==> "head"

      lint3.path ==> srcPath
      lint3.message ==> "not a total function"
      lint3.position.text ==> "get"
    }
  }
}
