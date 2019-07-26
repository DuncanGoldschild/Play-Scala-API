package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{Member, MemberUpdateRequest}


class MongoMemberRepository @Inject() (
                                       components: ControllerComponents,
                                       val reactiveMongoApi: ReactiveMongoApi
                                     ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("member"))

  def createOne(newMember: Member): Future[Option[Member]] = {
    val insertedMember = Member(newMember.username, newMember.password)
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

  def updateOne (username: String, newMember: MemberUpdateRequest): Future[Option[Unit]] = {
    val updatedMember = Member(username, newMember.password)
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