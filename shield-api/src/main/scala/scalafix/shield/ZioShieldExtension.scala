package scalafix.shield

import java.nio.file.Path

import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.{SemanticDocument, SyntacticDocument}

import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.{Failure, Success, Try}

class ZioShieldExtension(fullClasspath: List[Path],
                         semanticDbTargetRoot: Option[String] = None) {

  val classpath: Classpath = {
    val targetroot = semanticDbTargetRoot
      .map(option => Classpath(option))
      .getOrElse(Classpath(Nil))
    val baseClasspath = if (fullClasspath.isEmpty) {
      val roots = PathIO.workingDirectory :: Nil
      ClasspathOps.autoClasspath(roots)
    } else {
      Classpath(fullClasspath.map(AbsolutePath(_)))
    }
    baseClasspath ++ targetroot
  }

  val symtab: Either[Throwable, SymbolTable] =
    Try {
      ClasspathOps.newSymbolTable(
        classpath = classpath,
        out = System.out
      )
    }.toEither

  val classLoader: ClassLoader =
    ClasspathOps.toOrphanClassLoader(classpath)

  def semanticDocumentFromPath(
      doc: SyntacticDocument,
      path: Path): Either[Throwable, SemanticDocument] =
    for {
      s <- symtab
      doc <- Try {
        SemanticDocument.fromPath(
          doc,
          AbsolutePath(path).toRelative(PathIO.workingDirectory),
          classLoader,
          s
        )
      }.toEither
    } yield doc
}

object ZioShieldExtension {
  def semanticdbTargetRoot(scalacOptions: List[String]): Option[String] = {
    val flag = "-P:semanticdb:targetroot:"
    scalacOptions
      .find(_.startsWith(flag))
      .map(_.stripPrefix(flag))
  }
}
