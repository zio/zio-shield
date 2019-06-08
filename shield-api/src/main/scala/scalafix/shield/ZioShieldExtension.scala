package scalafix.shield

import scalafix.internal.reflect.ClasspathOps
import scalafix.v1.{SemanticDocument, SyntacticDocument}

import scala.meta.internal.io.PathIO
import scala.meta.internal.symtab.SymbolTable
import scala.meta.io.{AbsolutePath, Classpath}
import scala.util.{Failure, Success, Try}

object ZioShieldExtension {
  def symtab(scalacOptions: List[String]): Either[String, SymbolTable] = {
    Try(
      ClasspathOps.newSymbolTable(
        classpath = classpath(scalacOptions),
        out = System.out
      )
    ) match {
      case Success(symtab) =>
        Right(symtab)
      case Failure(e) =>
        Left(s"Unable to load symbol table: ${e.getMessage}")
    }
  }

  def semanticdbOption(scalacOptions: List[String],
                       name: String): Option[String] = {
    val flag = s"-P:semanticdb:$name:"
    scalacOptions
      .find(_.startsWith(flag))
      .map(_.stripPrefix(flag))
  }

  def classpath(scalacOptions: List[String]): Classpath = {
    val targetroot = semanticdbOption(scalacOptions, "targetroot")
      .map(option => Classpath(option))
      .getOrElse(Classpath(Nil))
    val baseClasspath = {
      val roots = PathIO.workingDirectory :: Nil
      ClasspathOps.autoClasspath(roots)
    }
    baseClasspath ++ targetroot
  }

  def classLoader(scalacOptions: List[String]): ClassLoader =
    ClasspathOps.toOrphanClassLoader(classpath(scalacOptions: List[String]))

  def semanticDocumentFromPath(
      doc: SyntacticDocument,
      path: java.nio.file.Path,
      scalacOptions: List[String]): Either[String, SemanticDocument] =
    for {
      s <- symtab(scalacOptions)
    } yield
      SemanticDocument.fromPath(
        doc,
        AbsolutePath(path).toRelative(PathIO.workingDirectory),
        classLoader(scalacOptions: List[String]),
        s
      )
}
