package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{Member, MemberAuth, MemberCreationRequest}
import reactivemongo.api.commands.WriteResult


class MongoMemberRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("member"))

  def createOne(newMember: MemberCreationRequest): Future[Option[Member]] = {
    val insertedMember = Member(newMember.username, newMember.password, newMember.boardsId, newMember.tasksId)
    findOne(newMember.username)
      .flatMap{ // checking if the username is already existing
        case None => {
          collection.flatMap(_.insert.one(insertedMember)).map{
            _ => Some(insertedMember)
          }
        }
        case Some(_) => Future.successful(None)
      }
  }

  override def deleteOne(username: String): Future[Option[Unit]] = {
    collection.flatMap(_.delete.one(idSelector(username)))
      .map(verifyUpdatedOneDocument)
  }

  def updateOne (username: String, newMember: MemberCreationRequest): Future[Option[Unit]] = {
    val updatedMember = Member(username, newMember.password, newMember.boardsId, newMember.tasksId)
    collection.flatMap(_.update.one(q = idSelector(username), u = updatedMember, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

  def findUser (memberAuth : MemberAuth): Future[Option[Member]] = {
      collection.flatMap(_.find(BSONDocument("username" -> memberAuth.username, "password" -> memberAuth.password)).one[Member])
  }

  def findByUsername(username: String): Future[Option[Member]] = {
    collection.flatMap(_.find(idSelector(username)).one[Member])
  }
  override def idSelector (username: String) = BSONDocument("username" -> username)
}