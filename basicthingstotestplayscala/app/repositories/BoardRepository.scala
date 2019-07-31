package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID
import models.{Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, NotFoundException}


class MongoBoardRepository @Inject() (
                                      components: ControllerComponents,
                                      val reactiveMongoApi: ReactiveMongoApi
                                    ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository[Board]{

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

  def update (boardUpdateRequestId : String, boardUpdateRequest: BoardUpdateRequest, username : String): Future[Either[Exception, Unit]] = {
    findOne(boardUpdateRequestId)
      .flatMap {
        case Some(board: Board) if isUsernameContainedInBoard(username, board) =>
          updateOne(boardUpdateRequestId, boardUpdateRequest)
            .map {
              case Some (_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def delete (boardId : String, username : String) : Future[Either[Exception, Unit]] = {
    findOne(boardId)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) =>
          deleteOne(boardId)
            .map {
              case Some (_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def find (boardId : String, username : String) : Future[Either[Exception, Board]] = {
    findOne(boardId)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) => Future.successful(Right(board))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }
}