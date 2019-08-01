package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import models.{ForbiddenException, Member, MemberUpdateRequest, NotFoundException}
import utils.ControllerUtils


class MongoMemberRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi,
                                       controllerUtils: ControllerUtils
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository [Member] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("member"))

  def createOne(newMember: Member): Future[Option[Member]] = {
    val insertedMember = Member(newMember.username, controllerUtils.bcryptService.cryptPassword(newMember.password))
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

  def updateOne (username: String, newMember: MemberUpdateRequest): Future[Option[Unit]] = {
    val updatedMember = Member(username, controllerUtils.bcryptService.cryptPassword(newMember.newPassword))
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

  def auth (memberAuth: Member): Future[Option[String]] = {
    findByUsername(memberAuth.username).map {
      case Some(member) if controllerUtils.bcryptService.checkPassword(memberAuth.password, member.password) => Some(controllerUtils.jwtService.generateToken(member.username))
      case _ => None
    }
  }

  def update (username: String, tokenUsername: String, memberUpdateRequest: MemberUpdateRequest): Future[Either[Exception, Unit]] = {
    findByUsername(username)
      .flatMap {
        case Some(member) if checkUserPermissions(username, tokenUsername)&& controllerUtils.bcryptService.checkPassword(memberUpdateRequest.password, member.password) =>
          updateOne(username, memberUpdateRequest)
            .map {
              case Some (_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def delete (username: String, tokenUsername: String): Future[Either[Exception, Unit]] = {
    if (checkUserPermissions(username, tokenUsername)) {
      deleteOne(username)
        .map {
          case Some(_) => Right()
          case None => Left(NotFoundException())
        }
    }
    else Future.successful(Left(ForbiddenException()))
  }

  private def checkUserPermissions (tokenUsername: String, username: String): Boolean = tokenUsername == username

  override def idSelector (username: String) = BSONDocument("username" -> username)
}