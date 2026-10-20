package socapp.domain

import java.time.LocalDate

final case class User(
    id: String,
    firstName: String,
    secondName: String,
    birthdate: Option[LocalDate],
    biography: Option[String],
    city: Option[String]
)

final case class UserRecord(
    id: String,
    firstName: String,
    secondName: String,
    birthdate: Option[LocalDate],
    biography: Option[String],
    city: Option[String],
    passwordHash: String
):
  def toUser: User =
    User(id, firstName, secondName, birthdate, biography, city)

final case class RegisterRequest(
    firstName: String,
    secondName: String,
    birthdate: Option[LocalDate],
    biography: Option[String],
    city: Option[String],
    password: String
)

final case class RegisterResponse(userId: String)

final case class LoginRequest(id: String, password: String)

final case class LoginResponse(token: String)

final case class Post(
    id: String,
    text: String,
    authorUserId: String
)

final case class CreatePostRequest(text: String)

final case class UpdatePostRequest(id: String, text: String)

final case class DialogMessage(
    from: String,
    to: String,
    text: String
)

final case class SendMessageRequest(text: String)

final case class ServerErrorBody(
    message: String,
    requestId: Option[String],
    code: Option[Int]
)

sealed trait ApiError
object ApiError:
  case object BadRequest extends ApiError
  case object Unauthorized extends ApiError
  case object NotFound extends ApiError
  final case class Internal(body: ServerErrorBody) extends ApiError
