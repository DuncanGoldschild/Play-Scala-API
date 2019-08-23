package services

import org.mindrot.jbcrypt.BCrypt

trait BCryptService {
  def cryptPassword(password: String): String
  def checkPassword(password: String, passwordHash: String): Boolean
}

object DefaultBCryptService extends BCryptService {

  override def cryptPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt())

  override def checkPassword(password: String, passwordHash: String): Boolean =
    BCrypt.checkpw(password, passwordHash)

}
