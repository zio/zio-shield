package com.vovapolu.shield

import java.nio.file.Path

import sbt.util.Logger
import scalafix.internal.config.PrintStreamReporter
import scalafix.internal.rule.NoValInForComprehension
import scalafix.internal.v1.Rules
import scalafix.shield.ZioShieldExtension
import scalafix.v1.SyntacticDocument

object ZioShield {

  val synRules: Rules = {
    println(
      Rules
        .all(this.getClass.getClassLoader)
        .map(_.name))

    val noValInFor = Rules
      .all(this.getClass.getClassLoader)
      .collect {
        case r: NoValInForComprehension => r
      }
      .head

    Rules(List(noValInFor))
  }

  def run(scalacOptions: List[String],
          files: List[Path],
          logger: Logger): Unit = {
    val reporter = PrintStreamReporter(System.out)

    files.foreach { f =>
      val synDoc = SyntacticDocument.fromInput(meta.Input.File(f))
      val sdoc = ZioShieldExtension.semanticDocumentFromPath(
        synDoc,
        f,
        scalacOptions
      )

      val (newDoc, msgs) = synRules.syntacticPatch(synDoc, suppress = false)
      logger.info(f.toAbsolutePath.toString)
      logger.info(newDoc)
      logger.info("---")
      msgs.foreach(reporter.lint)
    }
  }
}
