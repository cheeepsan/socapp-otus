package socapp.api

import cats.effect.IO
import org.http4s.HttpRoutes
import socapp.api.JsonCodecs.given
import socapp.domain.*
import socapp.service.SocialService
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final class HttpApi(service: SocialService):
  private val errorOut: EndpointOutput[ApiError] =
    oneOf[ApiError](
      oneOfVariantSingletonMatcher[ApiError](StatusCode.BadRequest)(ApiError.BadRequest),
      oneOfVariantSingletonMatcher[ApiError](StatusCode.Unauthorized)(ApiError.Unauthorized),
      oneOfVariantSingletonMatcher[ApiError](StatusCode.NotFound)(ApiError.NotFound),
      oneOfVariantValueMatcher(StatusCode.InternalServerError, jsonBody[ServerErrorBody].map(ApiError.Internal(_))(_.body)) {
        case ApiError.Internal(_) => true
      }
    )

  private val base = endpoint.errorOut(errorOut)

  private val secure =
    base.securityIn(auth.bearer[String]())

  private val login =
    base.post
      .in("login")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[LoginResponse])
      .description("Authenticate and receive a bearer token")

  private val register =
    base.post
      .in("user" / "register")
      .in(jsonBody[RegisterRequest])
      .out(jsonBody[RegisterResponse])
      .description("Register a new user")

  private val getUser =
    base.get
      .in("user" / "get" / path[String]("id"))
      .out(jsonBody[User])
      .description("Get user profile")

  private val searchUsers =
    base.get
      .in("user" / "search")
      .in(query[String]("first_name"))
      .in(query[String]("last_name"))
      .out(jsonBody[List[User]])
      .description("Search users by name prefixes")

  private val setFriend =
    secure.put
      .in("friend" / "set" / path[String]("user_id"))
      .out(emptyOutput)
      .description("Add a friend")

  private val deleteFriend =
    secure.put
      .in("friend" / "delete" / path[String]("user_id"))
      .out(emptyOutput)
      .description("Remove a friend")

  private val createPost =
    secure.post
      .in("post" / "create")
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[String])
      .description("Create a post")

  private val updatePost =
    secure.put
      .in("post" / "update")
      .in(jsonBody[UpdatePostRequest])
      .out(emptyOutput)
      .description("Update a post")

  private val deletePost =
    secure.put
      .in("post" / "delete" / path[String]("id"))
      .out(emptyOutput)
      .description("Delete a post")

  private val getPost =
    base.get
      .in("post" / "get" / path[String]("id"))
      .out(jsonBody[Post])
      .description("Get a post")

  private val feed =
    secure.get
      .in("post" / "feed")
      .in(query[Option[Int]]("offset"))
      .in(query[Option[Int]]("limit"))
      .out(jsonBody[List[Post]])
      .description("Friends post feed")

  private val sendDialog =
    secure.post
      .in("dialog" / path[String]("user_id") / "send")
      .in(jsonBody[SendMessageRequest])
      .out(emptyOutput)
      .description("Send a dialog message")

  private val listDialog =
    secure.get
      .in("dialog" / path[String]("user_id") / "list")
      .out(jsonBody[List[DialogMessage]])
      .description("List dialog with a user")

  private def recover[A](fa: IO[Either[ApiError, A]]): IO[Either[ApiError, A]] =
    fa.handleError { e =>
      Left(ApiError.Internal(ServerErrorBody(e.getMessage, None, Some(500))))
    }

  private val authLogic: String => IO[Either[ApiError, String]] =
    token => recover(service.authenticate(token))

  private val docs: List[Endpoint[?, ?, ?, ?, ?]] =
    List(
      login,
      register,
      getUser,
      searchUsers,
      setFriend,
      deleteFriend,
      createPost,
      updatePost,
      deletePost,
      getPost,
      feed,
      sendDialog,
      listDialog
    )

  private val swagger: List[ServerEndpoint[Any, IO]] =
    SwaggerInterpreter().fromEndpoints[IO](docs, "OTUS Highload Architect", "1.2.0")

  private val servers: List[ServerEndpoint[Any, IO]] =
    List[ServerEndpoint[Any, IO]](
      login.serverLogic(req => recover(service.login(req))),
      register.serverLogic(req => recover(service.register(req))),
      getUser.serverLogic(id => recover(service.getUser(id))),
      searchUsers.serverLogic { case (first, last) => recover(service.searchUsers(first, last)) },
      setFriend.serverSecurityLogic(authLogic).serverLogic { userId => friendId =>
        recover(service.addFriend(userId, friendId))
      },
      deleteFriend.serverSecurityLogic(authLogic).serverLogic { userId => friendId =>
        recover(service.deleteFriend(userId, friendId))
      },
      createPost.serverSecurityLogic(authLogic).serverLogic { userId => req =>
        recover(service.createPost(userId, req))
      },
      updatePost.serverSecurityLogic(authLogic).serverLogic { userId => req =>
        recover(service.updatePost(userId, req))
      },
      deletePost.serverSecurityLogic(authLogic).serverLogic { userId => postId =>
        recover(service.deletePost(userId, postId))
      },
      getPost.serverLogic(id => recover(service.getPost(id))),
      feed.serverSecurityLogic(authLogic).serverLogic { userId => (offset, limit) =>
        recover(service.feed(userId, offset, limit))
      },
      sendDialog.serverSecurityLogic(authLogic).serverLogic { userId => (to, req) =>
        recover(service.sendMessage(userId, to, req))
      },
      listDialog.serverSecurityLogic(authLogic).serverLogic { userId => otherId =>
        recover(service.listDialog(userId, otherId))
      }
    ) ++ swagger

  val routes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(servers)
