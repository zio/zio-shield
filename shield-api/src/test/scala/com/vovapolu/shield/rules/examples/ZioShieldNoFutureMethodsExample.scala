package com.vovapolu.shield.rules.examples

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ZioShieldNoFutureMethodsExample {
  def boom: Future[Unit] = Future { println("boom!") }
  val bam: Future[Unit] = Future { println("bam!") }
  def whaam = Future { println("whaam!") }
  val splash = Future { println("splash!") }
}
