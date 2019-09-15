package zio.shield.config

class InvalidConfig(msg: String) extends Exception(s"Invalid ZIO Shield config: $msg")
