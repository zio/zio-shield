package zio.shield.rules.examples

import zio._
import zio.stream._
import zio.blocking._

object ZioShieldBlocks {
  def helloWorld(): Unit = println("Hello world!")

  case class Env()
  case class Err()
  case class Val()

  FunctionIO.effect { case e => e } { (e: Env) =>
    helloWorld()
  }
  FunctionIO.effectTotal { (x: Env) =>
    helloWorld()
  }
  Task {
    helloWorld()
  }
  Task.effect {
    helloWorld()
  }
  Task.effectAsync { (cb: Task[Val] => Unit) =>
    helloWorld()
  }
  Task.effectAsyncInterrupt { (cb: Task[Val] => Unit) =>
    helloWorld()
    Right(ZIO.unit)
  }
  Task.effectAsyncM { (cb: Task[Val] => Unit) =>
    Task {
      helloWorld()
    }
  }
  Task.effectAsyncMaybe { (cb: Task[Val] => Unit) =>
    helloWorld()
    None
  }
  Task.effectTotal {
    helloWorld()
  }
  IO {
    helloWorld()
  }
  IO.effect {
    helloWorld()
  }
  IO.effectAsync { (cb: IO[Val, Err] => Unit) =>
    helloWorld()
  }
  IO.effectAsyncInterrupt { (cb: IO[Val, Err] => Unit) =>
    helloWorld()
    Right(ZIO.unit)
  }
  IO.effectAsyncM { (cb: IO[Val, Err] => Unit) =>
    IO {
      helloWorld()
    }
  }
  IO.effectAsyncMaybe { (cb: IO[Val, Err] => Unit) =>
    helloWorld()
    None
  }
  IO.effectTotal {
    helloWorld()
  }
  RIO {
    helloWorld()
  }
  RIO.effect {
    helloWorld()
  }
  RIO.effectAsync { (cb: RIO[Env, Val] => Unit) =>
    helloWorld()
  }
  RIO.effectAsyncInterrupt { (cb: RIO[Any, Val] => Unit) =>
    helloWorld()
    Right(ZIO.unit)
  }
  RIO.effectAsyncM { (cb: RIO[Env, Val] => Unit) =>
    IO {
      helloWorld()
    }
  }
  RIO.effectAsyncMaybe { (cb: RIO[Env, Val] => Unit) =>
    helloWorld()
    None
  }
  RIO.effectTotal {
    helloWorld()
  }
  UIO {
    helloWorld()
  }
  UIO.effectAsync { (cb: UIO[Val] => Unit) =>
    helloWorld()
  }
  UIO.effectAsyncInterrupt { (cb: UIO[Val] => Unit) =>
    helloWorld()
    Right(ZIO.unit)
  }
  UIO.effectAsyncM { (cb: UIO[Val] => Unit) =>
    UIO {
      helloWorld()
    }
  }
  UIO.effectAsyncMaybe { (cb: UIO[Val] => Unit) =>
    helloWorld()
    None
  }
  UIO.effectTotal {
    helloWorld()
  }
  ZIO {
    helloWorld()
  }
  ZIO.effect {
    helloWorld()
  }
  ZIO.effectAsync { (cb: ZIO[Env, Err, Val] => Unit) =>
    helloWorld()
  }
  ZIO.effectAsyncInterrupt { (cb: ZIO[Any, Err, Val] => Unit) =>
    helloWorld()
    Right(ZIO.unit)
  }
  ZIO.effectAsyncM { (cb: ZIO[Env, Err, Val] => Unit) =>
    ZIO {
      helloWorld()
    }
  }
  ZIO.effectAsyncMaybe { (cb: ZIO[Env, Err, Val] => Unit) =>
    helloWorld()
    None
  }
  ZIO.effectTotal {
    helloWorld()
  }
  Stream.effectAsync { (cb: IO[Option[Err], Val] => Unit) =>
    helloWorld()
  }
  Stream.effectAsyncInterrupt { (cb: IO[Option[Err], Val] => Unit) =>
    helloWorld()
    Right(ZStream.empty)
  }
  Stream.effectAsyncM { (cb: IO[Option[Err], Val] => Unit) =>
    IO {
      helloWorld()
    }
  }
  Stream.effectAsyncMaybe { (cb: IO[Option[Err], Val] => Unit) =>
    helloWorld()
    None
  }
  ZStream.effectAsync { (cb: ZIO[Env, Option[Err], Val] => Unit) =>
    helloWorld()
  }
  ZStream.effectAsyncInterrupt { (cb: ZIO[Any, Option[Err], Val] => Unit) =>
    helloWorld()
    Right(ZStream.empty)
  }
  ZStream.effectAsyncM { (cb: ZIO[Env, Option[Err], Val] => Unit) =>
    IO {
      helloWorld()
    }
  }
  ZStream.effectAsyncMaybe { (cb: ZIO[Env, Option[Err], Val] => Unit) =>
    helloWorld()
    None
  }
  effectBlocking {
    helloWorld()
  }
  effectBlockingCancelable {
    helloWorld()
  } {
    ZIO.unit
  }
}
