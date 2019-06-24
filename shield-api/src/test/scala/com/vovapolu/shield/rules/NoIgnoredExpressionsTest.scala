package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoIgnoredExpressionsTest extends ZioShieldTest(ZioShieldNoIgnoredExpressions) {
  val tests = Tests {
    test("example") {
      val List(lint1: Lint, lint2: Lint, lint3: Lint) = zioShieldInstance.run(srcPath)

      lint1.path ==> srcPath
      lint1.message ==> "ignored expression"
      lint1.position.text ==> """println("hi!")"""

      lint2.path ==> srcPath
      lint2.message ==> "ignored expression"
      lint2.position.text ==> "123"

      lint3.path ==> srcPath
      lint3.message ==> "ignored expression"
      lint3.position.text ==> "boom"
    }
  }
}
