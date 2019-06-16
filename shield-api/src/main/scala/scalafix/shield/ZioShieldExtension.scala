package scalafix.shield

import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.{SemanticDocument, SyntacticDocument}

import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.{Failure, Success, Try}

object ZioShieldExtension {
  def symtab(
      semanticDbTargetRoot: Option[String]): Either[String, SymbolTable] = {
    Try(
      ClasspathOps.newSymbolTable(
        classpath = classpath(semanticDbTargetRoot),
        out = System.out
      )
    ) match {
      case Success(symtab) =>
        Right(symtab)
      case Failure(e) =>
        Left(s"Unable to load symbol table: ${e.getMessage}")
    }
  }

  def semanticdbTargetRoot(scalacOptions: List[String]): Option[String] = {
    val flag = "-P:semanticdb:targetroot:"
    scalacOptions
      .find(_.startsWith(flag))
      .map(_.stripPrefix(flag))
  }

  def classpath(semanticDbTargetRoot: Option[String]): Classpath = {
    val targetroot = semanticDbTargetRoot
      .map(option => Classpath(option))
      .getOrElse(Classpath(Nil))
    val baseClasspath = {
      val roots = PathIO.workingDirectory :: Nil
      ClasspathOps.autoClasspath(roots)
    }
    baseClasspath ++ targetroot
  }

  def classLoader(semanticDbTargetRoot: Option[String]): ClassLoader =
    ClasspathOps.toOrphanClassLoader(classpath(semanticDbTargetRoot))

  def semanticDocumentFromPath(
      doc: SyntacticDocument,
      path: java.nio.file.Path,
      semanticDbTargetRoot: Option[String]): Either[String, SemanticDocument] =
    for {
      s <- symtab(semanticDbTargetRoot)
    } yield
      SemanticDocument.fromPath(
        doc,
        AbsolutePath(path).toRelative(PathIO.workingDirectory),
        classLoader(semanticDbTargetRoot),
        s
      )
}
