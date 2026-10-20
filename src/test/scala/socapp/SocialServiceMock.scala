package socapp

import cats.effect.IO
import socapp.domain.*
import socapp.service.SocialService

/** Override only the methods a test needs. Unstubbed calls fail fast. */
class SocialServiceMock extends SocialService:
  private def unused[A](method: String): IO[Either[ApiError, A]] =
    IO.raiseError(new NotImplementedError(s"Stub SocialService.$method in this test"))

  def register(req: RegisterRequest): IO[Either[ApiError, RegisterResponse]] =
    unused(s"register($req)")
  def login(req: LoginRequest): IO[Either[ApiError, LoginResponse]] =
    unused(s"login($req)")
  def authenticate(token: String): IO[Either[ApiError, String]] =
    unused(s"authenticate($token)")
  def getUser(id: String): IO[Either[ApiError, User]] =
    unused(s"getUser($id)")
  def searchUsers(firstName: String, lastName: String): IO[Either[ApiError, List[User]]] =
    unused(s"searchUsers($firstName, $lastName)")
  def addFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]] =
    unused(s"addFriend($userId, $friendId)")
  def deleteFriend(userId: String, friendId: String): IO[Either[ApiError, Unit]] =
    unused(s"deleteFriend($userId, $friendId)")
  def createPost(userId: String, req: CreatePostRequest): IO[Either[ApiError, String]] =
    unused(s"createPost($userId, $req)")
  def updatePost(userId: String, req: UpdatePostRequest): IO[Either[ApiError, Unit]] =
    unused(s"updatePost($userId, $req)")
  def deletePost(userId: String, postId: String): IO[Either[ApiError, Unit]] =
    unused(s"deletePost($userId, $postId)")
  def getPost(id: String): IO[Either[ApiError, Post]] =
    unused(s"getPost($id)")
  def feed(userId: String, offset: Option[Int], limit: Option[Int]): IO[Either[ApiError, List[Post]]] =
    unused(s"feed($userId, $offset, $limit)")
  def sendMessage(from: String, to: String, req: SendMessageRequest): IO[Either[ApiError, Unit]] =
    unused(s"sendMessage($from, $to, $req)")
  def listDialog(userId: String, otherId: String): IO[Either[ApiError, List[DialogMessage]]] =
    unused(s"listDialog($userId, $otherId)")
