package socapp.api

import io.circe.Codec
import io.circe.derivation.{Configuration, ConfiguredCodec}
import socapp.domain.*
import sttp.tapir.Schema

object JsonCodecs:
  given Configuration = Configuration.default.withSnakeCaseMemberNames

  given Codec[User] = ConfiguredCodec.derived
  given Codec[RegisterRequest] = ConfiguredCodec.derived
  given Codec[RegisterResponse] = ConfiguredCodec.derived
  given Codec[LoginRequest] = ConfiguredCodec.derived
  given Codec[LoginResponse] = ConfiguredCodec.derived
  given Codec[Post] = ConfiguredCodec.derived
  given Codec[CreatePostRequest] = ConfiguredCodec.derived
  given Codec[UpdatePostRequest] = ConfiguredCodec.derived
  given Codec[DialogMessage] = ConfiguredCodec.derived
  given Codec[SendMessageRequest] = ConfiguredCodec.derived
  given Codec[ServerErrorBody] = ConfiguredCodec.derived

  given Schema[User] = Schema.derived
  given Schema[RegisterRequest] = Schema.derived
  given Schema[RegisterResponse] = Schema.derived
  given Schema[LoginRequest] = Schema.derived
  given Schema[LoginResponse] = Schema.derived
  given Schema[Post] = Schema.derived
  given Schema[CreatePostRequest] = Schema.derived
  given Schema[UpdatePostRequest] = Schema.derived
  given Schema[DialogMessage] = Schema.derived
  given Schema[SendMessageRequest] = Schema.derived
  given Schema[ServerErrorBody] = Schema.derived
