package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoFutureMethodsTest extends ZioShieldTest(ZioShieldNoFutureMethods) {
  val tests = Tests {
    test("example") {
      println(zioShieldInstance.run(srcPath))
    }
  }
}
