package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoTypeCastingTest extends ZioShieldTest(ZioShieldNoTypeCasting) {
  val tests = Tests {
    test("example") {
      val List(lint1: Lint, lint2: Lint) = zioShieldInstance.run(srcPath)

      lint1.path ==> srcPath
      lint1.message ==> "type casting"
      lint1.position.text ==> "asInstanceOf"

      lint2.path ==> srcPath
      lint2.message ==> "type casting"
      lint2.position.text ==> "isInstanceOf"
    }
  }
}
