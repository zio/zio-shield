package zio.shield.config

import io.circe.Decoder
import io.circe.generic.semiauto._


case class Config(excludedRules: List[String] = List.empty,
                  excludedInferrers: List[String] = List.empty)

object Config {
  def configDecoder: Decoder[Config] = deriveDecoder
}
