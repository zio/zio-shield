package zio.shield.rules.examples

import zio.UIO

object ZioShieldNoIgnoredExpressionsExample {
  println("hi!")
  123
  def boom = "boom!"
  boom
  def nonIgnored    = "it's okay"
  val nonIgnoredVal = 123
  class NonIgnoredClass {}

  UIO {
    println("hi!")
    123
    def boom = "boom!"
    boom
    def nonIgnored    = "it's okay"
    val nonIgnoredVal = 123
    class NonIgnoredClass {}
  }
}
