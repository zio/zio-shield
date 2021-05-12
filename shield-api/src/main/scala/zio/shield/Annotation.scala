package zio.shield

import scala.annotation.StaticAnnotation

object Annotation {
  final class pure        extends StaticAnnotation
  final class impure      extends StaticAnnotation
  final class partial     extends StaticAnnotation
  final class total       extends StaticAnnotation
  final class nullable    extends StaticAnnotation
  final class nonNullable extends StaticAnnotation

  final class pureInterface  extends StaticAnnotation
  final class implementation extends StaticAnnotation
  final class businessLogic  extends StaticAnnotation
}
