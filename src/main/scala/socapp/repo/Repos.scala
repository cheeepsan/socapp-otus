package socapp.repo

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import socapp.domain.*

final class UserRepo(xa: Transactor[IO]):
  def insert(record: UserRecord): IO[Unit] =
    val account =
      sql"""
        INSERT INTO users (id, password_hash)
        VALUES (${record.id}, ${record.passwordHash})
      """.update.run
    val meta =
      sql"""
        INSERT INTO user_meta (user_id, first_name, second_name, birthdate, biography, city)
        VALUES (${record.id}, ${record.firstName}, ${record.secondName}, ${record.birthdate},
                ${record.biography}, ${record.city})
      """.update.run
    (account *> meta).transact(xa).void

  def findById(id: String): IO[Option[UserRecord]] =
    sql"""
      SELECT u.id, m.first_name, m.second_name, m.birthdate, m.biography, m.city, u.password_hash
      FROM users u
      JOIN user_meta m ON m.user_id = u.id
      WHERE u.id = $id
    """.query[UserRecord].option.transact(xa)

  def search(firstNamePrefix: String, lastNamePrefix: String): IO[List[User]] =
    val first = firstNamePrefix + "%"
    val last = lastNamePrefix + "%"
    sql"""
      SELECT u.id, m.first_name, m.second_name, m.birthdate, m.biography, m.city
      FROM users u
      JOIN user_meta m ON m.user_id = u.id
      WHERE m.first_name LIKE $first AND m.second_name LIKE $last
      ORDER BY u.id
    """.query[User].to[List].transact(xa)

final class SessionRepo(xa: Transactor[IO]):
  def insert(token: String, userId: String): IO[Unit] =
    sql"INSERT INTO sessions (token, user_id) VALUES ($token, $userId)".update.run.transact(xa).void

  def findUserId(token: String): IO[Option[String]] =
    sql"SELECT user_id FROM sessions WHERE token = $token".query[String].option.transact(xa)

final class FriendRepo(xa: Transactor[IO]):
  def add(userId: String, friendId: String): IO[Unit] =
    sql"""
      INSERT INTO friends (user_id, friend_id) VALUES ($userId, $friendId)
      ON CONFLICT DO NOTHING
    """.update.run.transact(xa).void

  def remove(userId: String, friendId: String): IO[Unit] =
    sql"DELETE FROM friends WHERE user_id = $userId AND friend_id = $friendId".update.run.transact(xa).void

final class PostRepo(xa: Transactor[IO]):
  def insert(post: Post): IO[Unit] =
    sql"""
      INSERT INTO posts (id, author_user_id, text)
      VALUES (${post.id}, ${post.authorUserId}, ${post.text})
    """.update.run.transact(xa).void

  def findById(id: String): IO[Option[Post]] =
    sql"SELECT id, text, author_user_id FROM posts WHERE id = $id"
      .query[Post]
      .option
      .transact(xa)

  def update(id: String, authorId: String, text: String): IO[Int] =
    sql"UPDATE posts SET text = $text WHERE id = $id AND author_user_id = $authorId"
      .update
      .run
      .transact(xa)

  def delete(id: String, authorId: String): IO[Int] =
    sql"DELETE FROM posts WHERE id = $id AND author_user_id = $authorId"
      .update
      .run
      .transact(xa)

  def feed(userId: String, offset: Int, limit: Int): IO[List[Post]] =
    sql"""
      SELECT p.id, p.text, p.author_user_id
      FROM posts p
      JOIN friends f ON f.friend_id = p.author_user_id
      WHERE f.user_id = $userId
      ORDER BY p.created_at DESC
      OFFSET $offset LIMIT $limit
    """.query[Post].to[List].transact(xa)

final class DialogRepo(xa: Transactor[IO]):
  def send(from: String, to: String, text: String): IO[Unit] =
    sql"""
      INSERT INTO dialog_messages (from_user, to_user, text)
      VALUES ($from, $to, $text)
    """.update.run.transact(xa).void

  def list(userA: String, userB: String): IO[List[DialogMessage]] =
    sql"""
      SELECT from_user, to_user, text
      FROM dialog_messages
      WHERE (from_user = $userA AND to_user = $userB)
         OR (from_user = $userB AND to_user = $userA)
      ORDER BY created_at ASC, id ASC
    """.query[DialogMessage].to[List].transact(xa)
