package zio.shield.config

import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.yaml.parser

import scala.util.Try


case class Config(excludedRules: List[String] = List.empty,
                  excludedInferrers: List[String] = List.empty)

object Config {
  implicit val configDecoder: Decoder[Config] = deriveDecoder

  lazy val empty: Config = Config()

  def fromFile(path: Path): Either[Throwable, Config] = {
    if (!Files.isRegularFile(path)) {
      Left(new FileNotFoundException())
    } else {
      for {
        str <- Try {
          new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
          }.toEither
        json <- parser.parse(str)
        config <- json.as[Config]
      } yield config
    }
  }
}
