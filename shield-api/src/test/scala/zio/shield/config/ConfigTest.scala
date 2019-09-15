package zio.shield.config

import utest._

object ConfigTest extends TestSuite {
  val tests = Tests {
    test("BasicConfig") {
      Config
        .fromString("""
          |excludedRules: [ZioShieldNoIgnoredExpressions]
          |excludedInferrers: [NullabilityInferrer]""".stripMargin)
        .right
        .get ==> Config(List("ZioShieldNoIgnoredExpressions"),
                        List("NullabilityInferrer"))
    }

    test("NoExcludeRules") {
      Config
        .fromString("excludedInferrers: [NullabilityInferrer]")
        .right
        .get ==> Config(List.empty, List("NullabilityInferrer"))
    }

    test("NoExcludedInferrers") {
      Config
        .fromString("excludedRules: [ZioShieldNoIgnoredExpressions]")
        .right
        .get ==> Config(List("ZioShieldNoIgnoredExpressions"), List.empty)
    }
  }
}
