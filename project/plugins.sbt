addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M6")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.14.10")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
)
