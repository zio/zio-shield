package com.vovapolu.shield

import java.nio.file.Path

import scalafix.lint.LintSeverity

sealed trait ZioShieldDiagnostic {
  import ZioShieldDiagnostic._

  def path: Path

  def consoleMessage: String = this match {
    case Patch(path, oldDoc, newDoc) =>
      s"Detected patch for ${path.toString}:\n$newDoc" // TODO should be replaced by actual patching command
    case Lint(path, pos, message) =>
      import scalafix.internal.util.PositionSyntax._
      pos.formatMessage(LintSeverity.Error.toString, message)
    case SemanticFailure(path, error) =>
      s"Unable to load SemanticDb information for ${path.toString}: $error. Semantic rules are disabled."
  }
}

object ZioShieldDiagnostic {
  final case class Patch(path: Path, oldDoc: String, newDoc: String)
      extends ZioShieldDiagnostic

  final case class Lint(path: Path, position: meta.Position, message: String)
      extends ZioShieldDiagnostic

  final case class SemanticFailure(path: Path, error: String)
      extends ZioShieldDiagnostic
}
