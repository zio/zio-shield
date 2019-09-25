package zio.shield.rules.examples

import java.io.File

/*
[info] [info]   java/io/File#toPath().
[info] [info]   java/lang/Class#getClassLoader().
[info] [info]   java/lang/ClassLoader#getResource().
[info] [info]   java/lang/IllegalStateException#`<init>`(+1).
[info] [info]   java/lang/Object#`<init>`().
[info] [info]   java/lang/Object#`==`().
[info] [info]   java/lang/Object#getClass().
[info] [info]   java/lang/String#`+`().
[info] [info]   java/lang/String#contains().
[info] [info]   java/lang/String#length().
[info] [info]   java/lang/String#replace().
[info] [info]   java/lang/String#replace(+1).
[info] [info]   java/lang/String#replaceAll().
[info] [info]   java/lang/String#startsWith(+1).
[info] [info]   java/lang/String#substring(+1).
[info] [info]   java/net/URL#getFile().
[info] [info]   java/nio/file/Files#createTempDirectory(+1).
[info] [info]   java/nio/file/Files#readAllLines(+1).
[info] [info]   java/nio/file/Files#write(+2).
[info] [info]   java/nio/file/Path#resolve(+1).
[info] [info]   java/nio/file/Path#toAbsolutePath().
[info] [info]   java/nio/file/Path#toFile().
[info] [info]   scala/Int#`!=`(+3).
[info] [info]   scala/Int#`-`(+3).
[info] [info]   scala/Int#`>`(+3).
[info] [info]   scala/Option#exists().
[info] [info]   scala/Option#map().
[info] [info]   scala/Predef.ArrowAssoc#`->`().
[info] [info]   scala/Predef.ArrowAssoc().
[info] [info]   scala/Predef.println(+1).
[info] [info]   scala/Some.
[info] [info]   scala/StringContext#s().
[info] [info]   scala/Tuple2#_2.
[info] [info]   scala/collection/LinearSeqOptimized#find().
[info] [info]   scala/collection/MapLike#values().
[info] [info]   scala/collection/SeqLike#isEmpty().
[info] [info]   scala/collection/SeqLike#size().
[info] [info]   scala/collection/SetLike#contains().
[info] [info]   scala/collection/TraversableLike#filter().
[info] [info]   scala/collection/TraversableLike#filterNot().
[info] [info]   scala/collection/TraversableLike#flatMap().
[info] [info]   scala/collection/TraversableLike#map().
[info] [info]   scala/collection/TraversableOnce#max().
[info] [info]   scala/collection/TraversableOnce#mkString(+1).
[info] [info]   scala/collection/TraversableOnce#toList().
[info] [info]   scala/collection/TraversableOnce#toMap().
[info] [info]   scala/collection/TraversableOnce#toSeq().
[info] [info]   scala/collection/TraversableOnce#toSet().
[info] [info]   scala/collection/convert/DecorateAsJava#bufferAsJavaListConverter().
[info] [info]   scala/collection/convert/DecorateAsScala#asScalaBufferConverter().
[info] [info]   scala/collection/convert/Decorators.AsJava#asJava().
[info] [info]   scala/collection/convert/Decorators.AsScala#asScala().
[info] [info]   scala/collection/generic/GenericCompanion#empty().
[info] [info]   scala/collection/immutable/List#flatMap().
[info] [info]   scala/collection/immutable/List#foreach().
[info] [info]   scala/collection/immutable/List#map().
[info] [info]   scala/collection/immutable/List#take().
[info] [info]   scala/collection/immutable/List.
[info] [info]   scala/collection/immutable/MapLike#filterKeys().
[info] [info]   scala/collection/immutable/MapLike#mapValues().
[info] [info]   scala/collection/immutable/StringLike#r().
[info] [info]   scala/collection/mutable/BufferLike#append().
[info] [info]   scala/collection/mutable/ListBuffer#result().
[info] [info]   scala/collection/mutable/ListBuffer.
[info] [info]   scala/math/Ordering#gt().
[info] [info]   scala/math/Ordering.by().
[info] [info]   scala/package.Ordering.
[info] [info]   scala/sys/process/Process.
[info] [info]   scala/sys/process/ProcessBuilder#`!`(+1).
[info] [info]   scala/util/Try#getOrElse().
[info] [info]   scala/util/Try.
[info] [info]   scala/util/matching/Regex#findFirstMatchIn().
[info] [info]   scala/util/matching/Regex.MatchData#group().

*/

object ZioShieldBadSymbols {
  val f = new File(".")
  f.toPath
}
