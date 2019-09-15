package zio.shield.rules.examples

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ZioShieldShowcase {

  def bigComputation(data: String): Future[Int] = Future {
    (1 to 1000000).sum
  }

  sealed abstract class Response

  case class BadResponse(error: String) extends Response
  case class GoodResponse(data: String) extends Response

  def sendImportantRequest(data: String): Unit = ???

  def alwaysGood(): Response = {
    GoodResponse("it's good, believe me")
  }

  def nullable(): Response = {
    GoodResponse(null)
  }

  def handleResponse(response: Response): Unit = {
    if (response.isInstanceOf[BadResponse]) {
      val badResponse = response.asInstanceOf[BadResponse]

      println(badResponse.error)
      throw new RuntimeException("Bad response!")
    } else if (response.isInstanceOf[GoodResponse]) {
      val goodResponse = response.asInstanceOf[GoodResponse]

      val result = bigComputation(goodResponse.data)

      result.foreach { sum =>
        handleResponse(alwaysGood())
        handleResponse(nullable())

        println(s"Total sum: $sum")
      }
    }
  }
}
