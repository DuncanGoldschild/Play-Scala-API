package services
import java.io.File

import com.typesafe.config.ConfigFactory
import pdi.jwt.{Jwt, JwtAlgorithm}
import play.libs.Json

import scala.util.{Failure, Success, Try}


trait JwtTokenGeneratorServices {
  def generateToken(u : String) : String
  def fetchUsername(token : String) : Option[String]
}

class JwtTokenGenerator extends JwtTokenGeneratorServices {

  val conf = ConfigFactory.parseFile(new File("conf/application.conf"))
  val secret = ConfigFactory.load(conf).getString("play.crypto.secret")

  override def generateToken(u : String): String = Jwt.encode(s"""{ "username" : "$u" }""", secret, JwtAlgorithm.HS256)

  override def fetchUsername(token: String): Option[String] = {
     Jwt.decode(token, secret, Seq(JwtAlgorithm.HS256)) match {
      case Success(x) => Some(Json.parse(x.content).get("username").asText)
      case Failure(_) => None
    }
  }
}