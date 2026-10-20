package socapp.service

import cats.effect.IO
import org.mindrot.jbcrypt.BCrypt
import socapp.domain.*
import socapp.repo.*

import java.util.UUID

trait SocialService:
  def register(req: RegisterRequest): IO[Either[ApiError, RegisterResponse]]
  def login(req: LoginRequest): IO[Either[ApiError, LoginResponse]]
  def authenticate(token: String): IO[Either[ApiError, String]]
  def getUser(id: String): IO[Either[ApiError, User]]
  def searchUsers(firstName: String, lastName: String): IO[Either[ApiError, List[User]]]
  def addFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]]
  def deleteFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]]
  def createPost(userId: String, req: CreatePostRequest): IO[Either[ApiError, String]]
  def updatePost(userId: String, req: UpdatePostRequest): IO[Either[ApiError, Unit]]
  def deletePost(userId: String, postId: String): IO[Either[ApiError, Unit]]
  def getPost(id: String): IO[Either[ApiError, Post]]
  def feed(userId: String, offset: Option[Int], limit: Option[Int]): IO[Either[ApiError, List[Post]]]
  def sendMessage(from: String, to: String, req: SendMessageRequest): IO[Either[ApiError, Unit]]
  def listDialog(userId: String, otherId: String): IO[Either[ApiError, List[DialogMessage]]]

final class SocialServiceLive(
    users: UserRepo,
    sessions: SessionRepo,
    friends: FriendRepo,
    posts: PostRepo,
    dialogs: DialogRepo
) extends SocialService:
  private def blank(s: String): Boolean = s.trim.isEmpty

  def register(req: RegisterRequest): IO[Either[ApiError, RegisterResponse]] =
    if blank(req.firstName) || blank(req.secondName) || blank(req.password) then
      IO.pure(Left(ApiError.BadRequest))
    else
      val id = UUID.randomUUID().toString
      val record = UserRecord(
        id = id,
        firstName = req.firstName,
        secondName = req.secondName,
        birthdate = req.birthdate,
        biography = req.biography,
        city = req.city,
        passwordHash = BCrypt.hashpw(req.password, BCrypt.gensalt())
      )
      users.insert(record).as(Right(RegisterResponse(id)))

  def login(req: LoginRequest): IO[Either[ApiError, LoginResponse]] =
    if blank(req.id) || blank(req.password) then IO.pure(Left(ApiError.BadRequest))
    else
      users.findById(req.id).flatMap {
        case None => IO.pure(Left(ApiError.NotFound))
        case Some(user) if !BCrypt.checkpw(req.password, user.passwordHash) =>
          IO.pure(Left(ApiError.BadRequest))
        case Some(user) =>
          val token = UUID.randomUUID().toString
          sessions.insert(token, user.id).as(Right(LoginResponse(token)))
      }

  def authenticate(token: String): IO[Either[ApiError, String]] =
    if blank(token) then IO.pure(Left(ApiError.Unauthorized))
    else sessions.findUserId(token).map(_.toRight(ApiError.Unauthorized))

  def getUser(id: String): IO[Either[ApiError, User]] =
    if blank(id) then IO.pure(Left(ApiError.BadRequest))
    else users.findById(id).map(_.map(_.toUser).toRight(ApiError.NotFound))

  def searchUsers(firstName: String, lastName: String): IO[Either[ApiError, List[User]]] =
    if blank(firstName) || blank(lastName) then IO.pure(Left(ApiError.BadRequest))
    else users.search(firstName, lastName).map(Right(_))

  def addFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]] =
    if blank(friendId) || friendId == userId then IO.pure(Left(ApiError.BadRequest))
    else
      users.findById(friendId).flatMap {
        case None    => IO.pure(Left(ApiError.BadRequest))
        case Some(_) => friends.add(userId, friendId).as(Right(()))
      }

  def deleteFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]] =
    if blank(friendId) then IO.pure(Left(ApiError.BadRequest))
    else friends.remove(userId, friendId).as(Right(()))

  def createPost(userId: String, req: CreatePostRequest): IO[Either[ApiError, String]] =
    if blank(req.text) then IO.pure(Left(ApiError.BadRequest))
    else
      val post = Post(UUID.randomUUID().toString, req.text, userId)
      posts.insert(post).as(Right(post.id))

  def updatePost(userId: String, req: UpdatePostRequest): IO[Either[ApiError, Unit]] =
    if blank(req.id) || blank(req.text) then IO.pure(Left(ApiError.BadRequest))
    else
      posts.update(req.id, userId, req.text).map {
        case 0 => Left(ApiError.BadRequest)
        case _ => Right(())
      }

  def deletePost(userId: String, postId: String): IO[Either[ApiError, Unit]] =
    if blank(postId) then IO.pure(Left(ApiError.BadRequest))
    else
      posts.delete(postId, userId).map {
        case 0 => Left(ApiError.BadRequest)
        case _ => Right(())
      }

  def getPost(id: String): IO[Either[ApiError, Post]] =
    if blank(id) then IO.pure(Left(ApiError.BadRequest))
    else posts.findById(id).map(_.toRight(ApiError.NotFound))

  def feed(userId: String, offset: Option[Int], limit: Option[Int]): IO[Either[ApiError, List[Post]]] =
    val off = offset.getOrElse(0)
    val lim = limit.getOrElse(10)
    if off < 0 || lim < 1 then IO.pure(Left(ApiError.BadRequest))
    else posts.feed(userId, off, lim).map(Right(_))

  def sendMessage(from: String, to: String, req: SendMessageRequest): IO[Either[ApiError, Unit]] =
    if blank(to) || blank(req.text) then IO.pure(Left(ApiError.BadRequest))
    else
      users.findById(to).flatMap {
        case None    => IO.pure(Left(ApiError.BadRequest))
        case Some(_) => dialogs.send(from, to, req.text).as(Right(()))
      }

  def listDialog(userId: String, otherId: String): IO[Either[ApiError, List[DialogMessage]]] =
    if blank(otherId) then IO.pure(Left(ApiError.BadRequest))
    else dialogs.list(userId, otherId).map(Right(_))
