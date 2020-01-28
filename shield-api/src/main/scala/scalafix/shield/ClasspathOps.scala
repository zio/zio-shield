// Taken from https://github.com/scalacenter/scalafix/blob/master/scalafix-reflect/src/main/scala/scalafix/internal/reflect/ClasspathOps.scala
package scalafix.shield

import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.meta.Classpath
import scala.meta.internal.symtab._
import scala.meta.io.AbsolutePath

object ClasspathOps {

  def newSymbolTable(
    classpath: Classpath
  ): SymbolTable =
    GlobalSymbolTable(classpath, includeJdk = true)

  private val META_INF   = Paths.get("META-INF")
  private val SEMANTICDB = Paths.get("semanticdb")

  private def isTargetroot(path: Path): Boolean =
    path.toFile.isDirectory &&
      path.resolve(META_INF).toFile.isDirectory &&
      path.resolve(META_INF).resolve(SEMANTICDB).toFile.isDirectory

  private def isJar(path: AbsolutePath): Boolean =
    path.isFile &&
      path.toFile.getName.endsWith(".jar")

  def autoClasspath(roots: List[AbsolutePath]): Classpath = {
    val buffer = List.newBuilder[AbsolutePath]
    val visitor = new SimpleFileVisitor[Path] {
      override def preVisitDirectory(
        dir: Path,
        attrs: BasicFileAttributes
      ): FileVisitResult =
        if (isTargetroot(dir)) {
          buffer += AbsolutePath(dir)
          FileVisitResult.SKIP_SUBTREE
        } else {
          FileVisitResult.CONTINUE
        }
    }
    roots.foreach(x => Files.walkFileTree(x.toNIO, visitor))
    roots.filter(isJar).foreach(buffer += _)
    Classpath(buffer.result())
  }

  def toOrphanClassLoader(classpath: Classpath): URLClassLoader =
    toClassLoaderWithParent(classpath, null)

  private def toClassLoaderWithParent(
    classpath: Classpath,
    parent: ClassLoader
  ): URLClassLoader = {
    val urls = classpath.entries.map(_.toNIO.toUri.toURL).toArray
    new URLClassLoader(urls, parent)
  }
}
