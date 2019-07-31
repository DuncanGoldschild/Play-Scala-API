package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import models.{BadRequestException, Member, MemberUpdateRequest}
import services.{BCryptServiceImpl, JwtGenerator}


class MongoMemberRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi,
                                       bcryptService : BCryptServiceImpl,
                                       jwtService : JwtGenerator
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [Member] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("member"))

  def createOne(newMember: Member): Future[Option[Member]] = {
    val insertedMember = Member(newMember.username, bcryptService.cryptPassword(newMember.password))
    findOne(newMember.username)
      .flatMap {
        case None => {
          collection.flatMap(_.insert.one(insertedMember)).map {
            _ => Some(insertedMember)
          }
        }
        case Some(_) => Future.successful(None)
      }
  }

  def auth (memberAuth: Member) : Future[Option[String]] = {
    findByUsername(memberAuth.username).map {
      case Some(member) if bcryptService.checkPassword(memberAuth.password, member.password) => Some(jwtService.generateToken(member.username))
      case _ => None
    }
  }

  def updateOne (username: String, newMember: MemberUpdateRequest): Future[Option[Unit]] = {
    val updatedMember = Member(username, bcryptService.cryptPassword(newMember.newPassword))
    collection.flatMap(_.update.one(q = idSelector(username), u = updatedMember, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

  def findByUsername(username: String): Future[Option[Member]] = {
    collection.flatMap(_.find(idSelector(username)).one[Member])
  }

  override def deleteOne(username: String): Future[Option[Unit]] = {
    collection.flatMap(_.delete.one(idSelector(username)))
      .map(verifyUpdatedOneDocument)
  }

  override def idSelector (username: String) = BSONDocument("username" -> username)
}