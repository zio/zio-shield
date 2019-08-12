package scalafix.shield

import java.nio.file.Path

import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.{SemanticDocument, SyntacticDocument}

import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.{Failure, Success, Try}

object ZioShieldExtension {
  def symtab(semanticDbTargetRoot: Option[String],
             fullClasspath: List[Path]): Either[Throwable, SymbolTable] =
    Try {
      ClasspathOps.newSymbolTable(
        classpath = classpath(semanticDbTargetRoot, fullClasspath),
        out = System.out
      )
    }.toEither

  def semanticdbTargetRoot(scalacOptions: List[String]): Option[String] = {
    val flag = "-P:semanticdb:targetroot:"
    scalacOptions
      .find(_.startsWith(flag))
      .map(_.stripPrefix(flag))
  }

  def classpath(semanticDbTargetRoot: Option[String],
                fullClasspath: List[Path]): Classpath = {
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

  def classLoader(semanticDbTargetRoot: Option[String],
                  fullClasspath: List[Path]): ClassLoader =
    ClasspathOps.toOrphanClassLoader(
      classpath(semanticDbTargetRoot, fullClasspath))

  def semanticDocumentFromPath(
      doc: SyntacticDocument,
      path: Path,
      semanticDbTargetRoot: Option[String],
      fullClasspath: List[Path]): Either[Throwable, SemanticDocument] =
    for {
      s <- symtab(semanticDbTargetRoot, fullClasspath)
      doc <- Try {
        SemanticDocument.fromPath(
          doc,
          AbsolutePath(path).toRelative(PathIO.workingDirectory),
          classLoader(semanticDbTargetRoot, fullClasspath),
          s
        )
      }.toEither
    } yield doc
}
