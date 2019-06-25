package com.vovapolu.shield.rules

import java.nio.file.{Path, Paths}

import com.vovapolu.shield.ZioShield
import scalafix.v1.Rule
import utest._

import scala.io.Source

object ConsoleMessagesTest extends TestSuite {
  val testsPath: Path = Paths.get(System.getProperty("user.dir"))

  val fullClasspath: List[Path] = {
    def allUrls(cl: ClassLoader): Array[java.net.URL] = cl match {
      case null                       => Array()
      case u: java.net.URLClassLoader => u.getURLs ++ allUrls(cl.getParent)
      case _                          => allUrls(cl.getParent)
    }

    val urls = allUrls(getClass.getClassLoader).toList
    urls.collect {
      case u if u.getPath.nonEmpty => Paths.get(u.getPath)
    }
  }

  def consoleMessageTest(rule: Rule): Unit = {
    val srcPath = testsPath.resolve(
      s"shield-api/src/test/scala/com/vovapolu/shield/rules/examples/${rule.name.toString}Example.scala")

    val zioShieldInstance =
      ZioShield(None, fullClasspath)(List.empty, List(rule))

    val consoleMessages =
      zioShieldInstance
        .run(srcPath)
        .map(_.consoleMessage.stripPrefix(s"${srcPath.getParent.toString}/"))

    val messagesResource = Source.fromResource(
      s"consoleMessages/${rule.name.toString}Example.messages")
    val targetMessages = messagesResource.mkString.split("\n---\n").toList

    consoleMessages ==> targetMessages
  }

  val tests = Tests {
    test("ZioShieldNoFutureMethods") {
      consoleMessageTest(ZioShieldNoFutureMethods)
    }
    test("ZioShieldNoIgnoredExpressions") {
      consoleMessageTest(ZioShieldNoIgnoredExpressions)
    }
    test("ZioShieldNoSideEffects") {
      consoleMessageTest(ZioShieldNoSideEffects)
    }
    test("ZioShieldNoThrowCatch") {
      consoleMessageTest(ZioShieldNoThrowCatch)
    }
    test("ZioShieldNoPartialFunctions") {
      consoleMessageTest(ZioShieldNoPartialFunctions)
    }
    test("ZioShieldNoNull") {
      consoleMessageTest(ZioShieldNoNull)
    }
    test("ZioShieldNoTypeCasting") {
      consoleMessageTest(ZioShieldNoTypeCasting)
    }
  }
}
