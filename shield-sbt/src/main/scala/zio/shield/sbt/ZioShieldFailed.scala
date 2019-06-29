package zio.shield.sbt

import sbt.internal.util.FeedbackProvidedException

final class ZioShieldFailed(errors: List[String])
    extends RuntimeException(errors.mkString("\n"))
    with FeedbackProvidedException
