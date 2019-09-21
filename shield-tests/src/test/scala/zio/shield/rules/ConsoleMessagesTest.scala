package zio.shield.rules

import java.nio.file.{Files, Path, Paths}

import scalafix.v1.Rule
import utest._
import zio.shield.semdocs.DirectSemanticDocumentLoader
import zio.shield.{ConfiguredZioShield, ZioShield, ZioShieldDiagnostic}

import scala.collection.mutable
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

  def consoleMessageTest(rules: List[Rule],
                         flowRules: List[FlowRule],
                         path: Path): Unit = {
    val zioShield = ConfiguredZioShield(
      DirectSemanticDocumentLoader(fullClasspath))(List.empty, rules, flowRules)
    runWithZioShield(zioShield, path)
  }

  def consoleMessageTest(flowRule: FlowRule, path: Path): Unit =
    consoleMessageTest(List.empty, List(flowRule), path)

  def consoleMessageTest(rule: Rule, path: Path): Unit =
    consoleMessageTest(List(rule), List.empty, path)

  private def runWithZioShield(zioShield: ConfiguredZioShield,
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
      (path.getParent,
       path.getFileName.toString.stripSuffix(".scala"),
       List(path))
    } else (Paths.get("/"), "", List.empty)

    val consoleMessages = {
      val diagnostics = mutable.Buffer[ZioShieldDiagnostic]()

      zioShield.updateCache(srcPaths)(diagnostics += _)
      zioShield.run(srcPaths)(diagnostics += _)

      diagnostics
        .sortWith {
          case (d1, d2) =>
            val pathCmp = d1.path.compareTo(d2.path)
            if (pathCmp != 0) {
              pathCmp < 0
            } else {
              d1.consoleMessage.compareTo(d1.consoleMessage) < 0
            }
        }
        .map(_.consoleMessage.stripPrefix(s"${parent.toString}/"))
    }

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
        s"shield-tests/src/test/scala/zio/shield/rules/examples/${utestPath.value.last}.scala")

    def autoDirPath(implicit utestPath: utest.framework.TestPath) =
      testsPath.resolve(
        s"shield-tests/src/test/scala/zio/shield/rules/examples/${utestPath.value.last}")

    test("ZioShieldNoFutureMethodsExample") {
      consoleMessageTest(ZioShieldNoFutureMethods, autoSrcPath)
    }
    test("ZioShieldNoIgnoredExpressionsExample") {
      consoleMessageTest(ZioShieldNoIgnoredExpressions, autoSrcPath)
    }
    test("noImpurity") {
      consoleMessageTest(ZioShieldNoImpurity, autoDirPath)
    }
    test("noPartial") {
      consoleMessageTest(ZioShieldNoPartial, autoDirPath)
    }
    test("noNull") {
      consoleMessageTest(ZioShieldNoNull, autoDirPath)
    }
    test("noIndirectUse") {
      consoleMessageTest(ZioShieldNoIndirectUse, autoDirPath)
    }
    test("ZioShieldNoTypeCastingExample") {
      consoleMessageTest(ZioShieldNoTypeCasting, autoSrcPath)
    }
    test("ZioShieldNoReflectionExample") {
      consoleMessageTest(ZioShieldNoReflection, autoSrcPath)
    }
    test("ZioShieldShowcase") {
      consoleMessageTest(ZioShield.allSemanticRules,
                         ZioShield.allFlowRules,
                         autoSrcPath)
    }
    test("ZioShieldBlocks") {
      consoleMessageTest(ZioShieldNoImpurity, autoSrcPath)
    }
  }
}
