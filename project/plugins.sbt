addSbtPlugin("com.typesafe.sbt"   % "sbt-git"       % "1.0.0")
addSbtPlugin("com.dwijnand"       % "sbt-dynver"    % "4.0.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"  % "2.3.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"       % "0.3.7")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"       % "1.1.2")
addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"  % "3.8.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage" % "1.6.1")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"      % "2.1.1")
addSbtPlugin("ch.epfl.scala"      % "sbt-bloop"     % "1.3.5")
addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"    % "0.4.2")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
