package zio.shield.semdocs

import java.nio.file.Path

import scalafix.shield.ZioShieldScalafixExtension
import scalafix.v1.{SemanticDocument, SyntacticDocument}
import zio.shield.SemanticDocumentLoader

case class DirectSemanticDocumentLoader(fullClasspath: List[Path])
    extends SemanticDocumentLoader {

  private val extension = new ZioShieldScalafixExtension(fullClasspath)

  def load(synDoc: SyntacticDocument,
           path: Path): Either[Throwable, SemanticDocument] =
    extension.semanticDocumentFromPath(synDoc, path)
}
