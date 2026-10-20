package socapp.api

import cats.effect.IO
import io.circe.parser.decode
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.headers.{`Content-Type`, Authorization}
import org.http4s.implicits.*
import socapp.SocialServiceMock
import socapp.api.JsonCodecs.given
import socapp.domain.*

import java.time.LocalDate

class EndpointsSpec extends CatsEffectSuite:

  private def app(service: SocialServiceMock) =
    HttpApi(service).routes.orNotFound

  private def jsonRequest(method: Method, path: Uri, body: String): Request[IO] =
    Request[IO](method, path)
      .withEntity(body)
      .withContentType(`Content-Type`(MediaType.application.json))

  test("GET /user/get/{id} returns mocked user"):
    val user = User(
      id = "u1",
      firstName = "Ivan",
      secondName = "Petrov",
      birthdate = Some(LocalDate.parse("1990-01-15")),
      biography = Some("hiking"),
      city = Some("Moscow")
    )
    val service = new SocialServiceMock:
      override def getUser(id: String) =
        IO.pure(if id == "u1" then Right(user) else Left(ApiError.NotFound))

    val request = Request[IO](Method.GET, uri"/user/get/u1")
    for
      response <- app(service).run(request)
      body <- response.as[String]
    yield
      assertEquals(response.status, Status.Ok)
      assertEquals(decode[User](body), Right(user))

  test("GET /user/get/{id} maps NotFound to HTTP 404"):
    val service = new SocialServiceMock:
      override def getUser(id: String) =
        IO.pure(Left(ApiError.NotFound))

    app(service).run(Request[IO](Method.GET, uri"/user/get/missing")).map { response =>
      assertEquals(response.status, Status.NotFound)
    }

  test("POST /login returns mocked token"):
    val service = new SocialServiceMock:
      override def login(req: LoginRequest) =
        IO.pure(
          if req.id == "u1" && req.password == "secret" then Right(LoginResponse("token-1"))
          else Left(ApiError.BadRequest)
        )

    val request = jsonRequest(Method.POST, uri"/login", """{"id":"u1","password":"secret"}""")
    for
      response <- app(service).run(request)
      body <- response.as[String]
    yield
      assertEquals(response.status, Status.Ok)
      assertEquals(decode[LoginResponse](body), Right(LoginResponse("token-1")))

  test("PUT /friend/set/{id} without token returns 401"):
    val service = new SocialServiceMock:
      override def authenticate(token: String) =
        IO.pure(Left(ApiError.Unauthorized))

    val request = Request[IO](Method.PUT, uri"/friend/set/u2")
    app(service).run(request).map { response =>
      assertEquals(response.status, Status.Unauthorized)
    }

  test("PUT /friend/set/{id} with mocked auth returns 200"):
    val service = new SocialServiceMock:
      override def authenticate(token: String) =
        IO.pure(if token == "token-1" then Right("u1") else Left(ApiError.Unauthorized))
      override def addFriend(userId: String, friendId: String) =
        IO.pure(if userId == "u1" && friendId == "u2" then Right(()) else Left(ApiError.BadRequest))

    val request = Request[IO](Method.PUT, uri"/friend/set/u2")
      .putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, "token-1")))
    app(service).run(request).map { response =>
      assertEquals(response.status, Status.Ok)
    }
