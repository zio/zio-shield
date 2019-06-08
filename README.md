# ZIO Shield
Enforce best coding practices with ZIO. 
Powered by [Scalafix](https://scalacenter.github.io/scalafix/) and [Scalazzi rules](https://github.com/scalaz/scalazzi).

## Installation 

Add `zio-shield` sbt plugin to your `project/plugins.sbt`:
```sbt
addSbtPlugin("com.vovapolu" % "zio-shield" % "<version>")
```

You will also need `semanticdb-scalac` compiler plugin and `-Yrangepos` scalac option in your `build.sbt`:
```sbt
addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.1.0" cross CrossVersion.full),
scalacOptions += "-Yrangepos"
```

## Usage

Just run `shield` command after initial compilation and it will statically analyze your code for potential errors and bugs. 
```
[warn] .../example/src/main/scala/example/Example.scala:6:13: error: asInstanceOf casts are disabled, use pattern matching instead
[warn]   1.asInstanceOf[Long]
[warn]     ^^^^^^^^^^^^
```

## Configuration

You can set `shieldFatalWarnings` sbt setting to make all warnings fatal,
blocking you build if there are any errors detected by ZIO Shield. 
```sbt
shieldFatalWarnings := true
```
```
[error] (example / Compile / shield) com.vovapolu.shield.ZioShieldFailed: .../example/src/main/scala/example/Example.scala:6:13: error: asInstanceOf casts are disabled, use pattern matching instead
[error]   val b = 1.asInstanceOf[Long]
[error]             ^^^^^^^^^^^^
```
