package zio.shield.rules

import java.nio.file.{Files, Path, Paths}

import scalafix.v1.Rule
import utest._
import zio.shield.{ConfiguredZioShield, ZioShield}
import zio.shield.tag.TagChecker

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

  val verbose = true

  def consoleMessageTest(zioShieldRule: TagChecker => Rule,
                         path: Path): Unit = {
    val instance = ZioShield(None, fullClasspath).apply(
      semanticZioShieldRules = List(zioShieldRule))
    runWithInstance(instance, path)
  }

  def consoleMessageTest(rule: Rule, path: Path): Unit = {
    val instance =
      ZioShield(None, fullClasspath).apply(semanticRules = List(rule))
    runWithInstance(instance, path)
  }

  private def runWithInstance(zioShieldInstance: ConfiguredZioShield,
                              path: Path): Unit = {
    val (parent, name, srcPaths) = if (Files.isDirectory(path)) {
      import scala.collection.JavaConverters._
      (path,
       path.getFileName.toString,
       Files
         .list(path)
         .filter(f => Files.isRegularFile(f))
         .iterator()
         .asScala
         .toList)
    } else if (Files.isRegularFile(path)) {
      (path.getParent, path.getFileName.toString.stripSuffix(".scala"), List(path))
    } else (Paths.get("/"), "", List.empty)

    val consoleMessages =
      zioShieldInstance
        .run(srcPaths)
        .map(_.consoleMessage.stripPrefix(s"${parent.toString}/"))

    val messagesResource =
      Source.fromResource(s"consoleMessages/$name.messages")
    val targetMessages = messagesResource.mkString.split("\n---\n").toList

    if (verbose) {
      if (consoleMessages != targetMessages) {
        val msg =
          s"""Messages are not equal
             |
             |Expected messages:
             |${targetMessages.mkString("\n---\n")}
             |
             |Actual messages:
             |${consoleMessages.mkString("\n---\n")}""".stripMargin
        Predef.assert(false, msg)
      }
    } else {
      consoleMessages ==> targetMessages
    }
  }

  val tests = Tests {

    def autoSrcPath(implicit utestPath: utest.framework.TestPath) =
      testsPath.resolve(
        s"shield-api/src/test/scala/zio/shield/rules/examples/${utestPath.value.last}Example.scala")

    def autoDirPath(implicit utestPath: utest.framework.TestPath) =
      testsPath.resolve(
        s"shield-api/src/test/scala/zio/shield/rules/examples/${utestPath.value.last}")

    test("ZioShieldNoFutureMethods") {
      consoleMessageTest(ZioShieldNoFutureMethods, autoSrcPath)
    }
    test("ZioShieldNoIgnoredExpressions") {
      consoleMessageTest(ZioShieldNoIgnoredExpressions, autoSrcPath)
    }
    test("ZioShieldNoSideEffects") {
      consoleMessageTest(ZioShieldNoSideEffects, autoSrcPath)
    }
    test("ZioShieldNoThrowCatch") {
      consoleMessageTest(ZioShieldNoThrowCatch, autoSrcPath)
    }
    test("ZioShieldNoPartialFunctions") {
      consoleMessageTest(ZioShieldNoPartialFunctions, autoSrcPath)
    }
    test("noNull") {
      consoleMessageTest(tc => new ZioShieldNoNull(tc), autoDirPath)
    }
    test("ZioShieldNoTypeCasting") {
      consoleMessageTest(ZioShieldNoTypeCasting, autoSrcPath)
    }
    test("ZioShieldNoReflection") {
      consoleMessageTest(ZioShieldNoReflection, autoSrcPath)
    }
  }
}
