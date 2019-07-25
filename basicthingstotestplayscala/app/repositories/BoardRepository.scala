package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID
import models.{Board, BoardCreationRequest, BoardUpdateRequest}


class MongoBoardRepository @Inject() (
                                      components: ControllerComponents,
                                      val reactiveMongoApi: ReactiveMongoApi
                                    ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GlobalRepository {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("board"))

  def createOne(newBoard: BoardCreationRequest, username : String): Future[Board] = {
    val insertedBoard = Board(BSONObjectID.generate().stringify, newBoard.label, Seq(username))
    collection.flatMap(_.insert.one(insertedBoard)).map { _ => insertedBoard }
  }

  def updateOne (id: String, newBoard: BoardUpdateRequest): Future[Option[Unit]] = {
    val updatedBoard = Board(id, newBoard.label, newBoard.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedBoard, upsert = false, multi = false))
      .map (verifyUpdatedOneDocument)
  }

  def findOneBoard(id: String): Future[Option[Board]] = {
    collection.flatMap(_.find(idSelector(id)).one[Board])
  }

}