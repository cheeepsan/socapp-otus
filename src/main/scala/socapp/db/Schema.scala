package socapp.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*

object Schema:
  private val ddl: ConnectionIO[Unit] =
    for
      _ <- sql"""
        CREATE TABLE IF NOT EXISTS users (
          id VARCHAR PRIMARY KEY,
          password_hash VARCHAR NOT NULL
        )""".update.run

      _ <- sql"""
        CREATE TABLE IF NOT EXISTS user_meta (
          user_id VARCHAR PRIMARY KEY REFERENCES users(id),
          first_name VARCHAR NOT NULL,
          second_name VARCHAR NOT NULL,
          birthdate DATE,
          biography TEXT,
          city VARCHAR,
          interests TEXT
        )""".update.run

      _ <- sql"""
        CREATE INDEX IF NOT EXISTS users_name_search_idx
          ON user_meta (first_name text_pattern_ops, second_name text_pattern_ops)
      """.update.run

      _ <- sql"""
        CREATE TABLE IF NOT EXISTS sessions (
          token VARCHAR PRIMARY KEY,
          user_id VARCHAR NOT NULL REFERENCES users(id)
        )
      """.update.run

      _ <- sql"""
        CREATE TABLE IF NOT EXISTS friends (
          user_id VARCHAR NOT NULL REFERENCES users(id),
          friend_id VARCHAR NOT NULL REFERENCES users(id),
          PRIMARY KEY (user_id, friend_id)
        )
      """.update.run

      _ <- sql"""
        CREATE TABLE IF NOT EXISTS posts (
          id VARCHAR PRIMARY KEY,
          author_user_id VARCHAR NOT NULL REFERENCES users(id),
          text TEXT NOT NULL,
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      """.update.run

      _ <- sql"""
        CREATE INDEX IF NOT EXISTS posts_author_created_idx
          ON posts (author_user_id, created_at DESC)
      """.update.run

      _ <- sql"""
        CREATE TABLE IF NOT EXISTS dialog_messages (
          id BIGSERIAL PRIMARY KEY,
          from_user VARCHAR NOT NULL REFERENCES users(id),
          to_user VARCHAR NOT NULL REFERENCES users(id),
          text TEXT NOT NULL,
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      """.update.run

      _ <- sql"""
        CREATE INDEX IF NOT EXISTS dialog_pair_idx
          ON dialog_messages (from_user, to_user, created_at)
      """.update.run
    yield ()

  def migrate(xa: Transactor[IO]): IO[Unit] =
    ddl.transact(xa)
