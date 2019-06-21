package com.vovapolu.shield.rules

import java.nio.file.Paths

import com.vovapolu.shield.ZioShield
import com.vovapolu.shield.ZioShieldDiagnostic.Lint
import utest._

object NoNullTest extends TestSuite {
  val tests = Tests {
    test("NoNullExample") {
      val testPath = Paths.get(System.getProperty("user.dir"))
      val srcPath = testPath.resolve(
        "shield-api/src/test/scala/com/vovapolu/shield/rules/NoNullExample.scala")
      val zioShield = ZioShield(None)(List.empty, List(new ZioShieldNoNull))

      val res = zioShield.run(srcPath)

      res ==> List(
        Lint(srcPath,
             meta.Position.Range(meta.Input.File(srcPath), 3, 10, 3, 14),
             "null is forbidden")
      )
    }
  }
}
