addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M6")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
