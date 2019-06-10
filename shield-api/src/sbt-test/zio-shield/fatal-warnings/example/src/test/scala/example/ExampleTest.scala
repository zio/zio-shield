package example

object ExampleTest {
  val b = 1.asInstanceOf[Long]

  object WeekDay extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
  }
}
