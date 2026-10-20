package socapp.config

import com.typesafe.config.Config

final class DbConfig(config: Config):
  val host: String = config.getString("host")
  val port: Int = config.getInt("port")
  val name: String = config.getString("name")
  val user: String = config.getString("user")
  val password: String = config.getString("password")

  def jdbcUrl: String = s"jdbc:postgresql://$host:$port/$name"

final class AppConfig(config: Config):
  val httpHost: String = config.getString("http.host")
  val httpPort: Int = config.getInt("http.port")
  val db: DbConfig = DbConfig(config.getConfig("db"))
