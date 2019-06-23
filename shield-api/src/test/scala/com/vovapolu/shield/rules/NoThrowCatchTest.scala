package com.vovapolu.shield.rules

import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoThrowCatchTest extends ZioShieldTest(ZioShieldNoThrowCatch) {
  val tests = Tests {
    test("example") {
      val List(lint1: Lint, lint2: Lint) = zioShieldInstance.run(srcPath)

      lint1.path ==> srcPath
      lint1.message ==> "try/catch"
      lint1.position.text ==> """try {
                                |    List().head
                                |  } catch {
                                |    case e: Exception =>
                                |  }""".stripMargin
    }
  }
}
