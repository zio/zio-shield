package zio.shield.detector

import java.io.File
import java.lang.reflect.Method
import java.nio.file.{Files, Paths, StandardOpenOption}

import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

object Detector extends App {
  val selectedJavaPackages = Source
    .fromResource("selected_java_packages.txt")
    .getLines
    .toList

  val javaClasses =
    Source
      .fromResource("all_java_classes.txt")
      .getLines
      .filter(cls => selectedJavaPackages.exists(cls.startsWith))
      .flatMap[Class[_]] { className =>
        Try {
          Class.forName(className, true, null)
        } match {
          case Failure(e) =>
            println(s"Error while loading ${className}: ${e.toString}")
            None
          case Success(cls) => Some(cls)
        }
      }
      .toList

  def methodsFilter(method: Method): Boolean = true
  def classFilter(cls: Class[_]): Boolean = true

  val classLoadersList =
    List(ClasspathHelper.contextClassLoader, ClasspathHelper.staticClassLoader)
  lazy val reflections = new Reflections(
    new ConfigurationBuilder()
      .setScanners(new SubTypesScanner(false), new ResourcesScanner)
      .setUrls(ClasspathHelper.forClassLoader(classLoadersList: _*))
      .filterInputsBy(
        new FilterBuilder().include(FilterBuilder.prefix("scala"))))

  lazy val scalaClasses = reflections.getSubTypesOf(classOf[Any]).asScala.toList

  val partialMethods = (javaClasses ++ scalaClasses)
    .filter(classFilter)
    .flatMap { cls =>
      cls.getDeclaredMethods.filter(methodsFilter).collect {
        case m if m.getExceptionTypes.nonEmpty => s"${cls.getName}.${m.getName}"
      }
    }
    .distinct
    .sorted

  val partialFile = Paths.get("partial_methods.txt")

  Files.write(partialFile,
              partialMethods.mkString("\n").getBytes(),
              StandardOpenOption.CREATE)
}
