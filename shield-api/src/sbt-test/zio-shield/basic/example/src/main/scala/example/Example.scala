package example

import java.util.Map
import scala.concurrent.Future

object Example {
  for {
    e <- List(1, 2, 3)
    val e2 = e
  } yield e2
}
