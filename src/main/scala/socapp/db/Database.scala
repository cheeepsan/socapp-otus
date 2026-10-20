package socapp.db

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import socapp.config.DbConfig

object Database:
  def transactor(config: DbConfig): Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](8)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = config.jdbcUrl,
        user = config.user,
        pass = config.password,
        connectEC = ce
      )
    yield xa
