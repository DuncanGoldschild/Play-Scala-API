package services

import scala.util.{Failure, Success}
import java.io.File

import com.typesafe.config.ConfigFactory
import pdi.jwt.{Jwt, JwtAlgorithm}

import play.libs.Json

trait JwtService {
  def generateToken(u: String): String
  def getUsernameFromToken(token: String): Option[String]
}

object DefaultJwtService extends JwtService {

  private val conf = ConfigFactory.parseFile(new File("conf/application.conf"))
  private val secret = ConfigFactory.load(conf).getString("play.crypto.secret")

  override def generateToken(u: String): String =
    Jwt.encode(s"""{ "username": "$u" }""", secret, JwtAlgorithm.HS256)

  override def getUsernameFromToken(token: String): Option[String] = {
     Jwt.decode(token, secret, Seq(JwtAlgorithm.HS256)) match {
      case Success(x) => Some(Json.parse(x.content).get("username").asText)
      case Failure(_) => None
    }
  }

}