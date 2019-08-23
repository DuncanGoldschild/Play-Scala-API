package models

import play.api.libs.json.{Json, Writes}

case class User(username: String)

object User {
  implicit val writes: Writes[User] = Json.writes[User]
}

case class Token(token: String)

object Token {
  implicit val writes: Writes[Token] = Json.writes[Token]
}
