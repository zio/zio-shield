package com.vovapolu.shield.rules

import java.nio.file.{Path, Paths}

import com.vovapolu.shield.{ConfiguredZioShield, ZioShield}
import scalafix.v1.Rule
import utest.TestSuite

abstract class ZioShieldTest(val rule: Rule) extends TestSuite {
  lazy val testPath: Path = Paths.get(System.getProperty("user.dir"))

  lazy val srcPath: Path = testPath.resolve(
    s"shield-api/src/test/scala/com/vovapolu/shield/rules/examples/${rule.name.toString}Example.scala")

  lazy val zioShieldInstance: ConfiguredZioShield =
    ZioShield(None)(List.empty, List(rule))

}
