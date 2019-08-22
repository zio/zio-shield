package zio.shield.sbt

import sbt.internal.util.FeedbackProvidedException

final class ZioShieldFailed
    extends RuntimeException("Zio Shield failed. See logs for detailed errors.")
    with FeedbackProvidedException
