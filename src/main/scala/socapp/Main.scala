package socapp

import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.{Host, Port}
import com.typesafe.config.ConfigFactory
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import socapp.api.HttpApi
import socapp.config.AppConfig
import socapp.db.{Database, Schema}
import socapp.repo.*
import socapp.service.SocialServiceLive

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    val config = AppConfig(ConfigFactory.load())
    val resources =
      for
        xa <- Database.transactor(config.db)
        _ <- Resource.eval(Schema.migrate(xa))
        service = SocialServiceLive(
          UserRepo(xa),
          SessionRepo(xa),
          FriendRepo(xa),
          PostRepo(xa),
          DialogRepo(xa)
        )
        httpApp = Logger.httpApp(logHeaders = true, logBody = false)(
          HttpApi(service).routes.orNotFound
        )
        host <- Resource.eval(
          IO.fromOption(Host.fromString(config.httpHost))(
            new IllegalArgumentException(s"Invalid HTTP_HOST: ${config.httpHost}")
          )
        )
        port <- Resource.eval(
          IO.fromOption(Port.fromInt(config.httpPort))(
            new IllegalArgumentException(s"Invalid HTTP_PORT: ${config.httpPort}")
          )
        )
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpApp(httpApp)
          .build
      yield ()

    Slf4jLogger.create[IO].flatMap { logger =>
      logger.info(s"Starting socapp on ${config.httpHost}:${config.httpPort}") *>
        resources.useForever
    }
