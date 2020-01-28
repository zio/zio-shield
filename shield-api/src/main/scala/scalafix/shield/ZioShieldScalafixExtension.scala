package scalafix.shield

import java.nio.file.Path

import scalafix.internal.v1.InternalSemanticDoc
import scalafix.v1.{SemanticDocument, SyntacticDocument}

import scala.meta.internal.io.PathIO
import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.Try

class ZioShieldScalafixExtension(fullClasspath: List[Path]) {

  private val classpath: Classpath =
    if (fullClasspath.isEmpty) {
      val roots = PathIO.workingDirectory :: Nil
      ClasspathOps.autoClasspath(roots)
    } else {
      Classpath(fullClasspath.map(AbsolutePath(_)))
    }

  private val symtab: SymbolTable =
    ClasspathOps.newSymbolTable(
      classpath = classpath,
      out = System.out
    )

  private val classLoader: ClassLoader =
    ClasspathOps.toOrphanClassLoader(classpath)

  def semanticDocumentFromPath(
      doc: SyntacticDocument,
      path: Path): Either[Throwable, SemanticDocument] =
    for {
      doc <- Try {
        SemanticDocument.fromPath(
          doc,
          AbsolutePath(path).toRelative(PathIO.workingDirectory),
          classLoader,
          symtab,
          () => None
        )
      }.toEither
    } yield doc

  def semanticDocumentFromTextDocument(
      doc: SyntacticDocument,
      textDoc: TextDocument): SemanticDocument =
    new SemanticDocument(new InternalSemanticDoc(doc, textDoc, symtab))
}
