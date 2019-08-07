package repositories

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import models.{BadRequestException, Board, BoardCreationRequest, BoardUpdateRequest, ForbiddenException, NotFoundException, TasksList}
import reactivemongo.api.{Cursor, ReadPreference}

class MongoBoardRepository @Inject() (
                                      components: ControllerComponents,
                                      val reactiveMongoApi: ReactiveMongoApi,
                                      memberRepository: MongoMemberRepository
                                    ) extends AbstractController(components)
  with MongoController
  with ReactiveMongoComponents
  with GenericCRUDRepository[Board] {

  override def collection: Future[BSONCollection] =
    database.map(_.collection[BSONCollection]("board"))

  def createOne(newBoard: BoardCreationRequest, username: String): Future[Board] = {
    val insertedBoard = Board(BSONObjectID.generate().stringify, newBoard.label, Seq(username))
    collection.flatMap(_.insert.one(insertedBoard)).map { _ => insertedBoard }
  }

  def updateOne(id: String, newBoard: BoardUpdateRequest): Future[Option[Unit]] = {
    val updatedBoard = Board(id, newBoard.label, newBoard.membersUsername)
    collection.flatMap(_.update.one(q = idSelector(id), u = updatedBoard, upsert = false, multi = false))
      .map(verifyUpdatedOneDocument)
  }

  def update(boardUpdateRequestId: String, boardUpdateRequest: BoardUpdateRequest, username: String): Future[Either[Exception, Unit]] = {
    findOne(boardUpdateRequestId)
      .flatMap {
        case Some(board: Board) if isUsernameContainedInBoard(username, board) =>
          updateOne(boardUpdateRequestId, boardUpdateRequest)
            .map {
              case Some(_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def delete(boardId: String, username: String): Future[Either[Exception, Unit]] = {
    findOne(boardId)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) =>
          deleteOne(boardId)
            .map {
              case Some(_) => Right()
            }
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def find(boardId: String, username: String): Future[Either[Exception, Board]] = {
    findOne(boardId)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) => Future.successful(Right(board))
        case None => Future.successful(Left(NotFoundException()))
        case _ => Future.successful(Left(ForbiddenException()))
      }
  }

  def listAllLists(boardId: String, username: String): Future[Either[Exception, List[TasksList]]] = {
    findOne(boardId)
      .flatMap {
        case Some(board: Board) if isUsernameContainedInBoard(username, board) =>
          listAllListsFromBoardId(boardId)
            .map {
              Right(_)
            }
        case None => Future.successful(Left(NotFoundException("Board not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this board")))
      }
  }

  def addOneMemberToBoard(id: String, addedUsername: String): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$addToSet" -> BSONDocument("membersUsername" -> addedUsername)),
      fetchNewObject = true)
      .map {
        _.result[Board]
          .map {
            _ =>
          }
      }
    )
  }

  def deleteOneMemberFromBoard(id: String, deletedUsername: String): Future[Option[Unit]] = {
    collection.flatMap(_.findAndUpdate(
      idSelector(id),
      BSONDocument("$pull" -> BSONDocument("membersUsername" -> deletedUsername)),
      fetchNewObject = true)
      .map {
        _.result[Board]
          .map {
            _ =>
          }
      }
    )
  }

  def addMember(id: String, username: String, addedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) =>
          if (isUsernameContainedInBoard(addedMemberUsername, board)) Future.successful(Left(BadRequestException("User already has access to this board")))
          else memberRepository.findByUsername(addedMemberUsername).flatMap {
            case Some(_) =>
              addOneMemberToBoard(id, addedMemberUsername)
                .map {
                  _ => Right()
                }
            case None => Future.successful(Left(BadRequestException("User does not exist")))
          }

        case None => Future.successful(Left(NotFoundException("Board not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this board")))
      }
  }

  def deleteMember(id: String, username: String, deletedMemberUsername: String): Future[Either[Exception, Unit]] = {
    findOne(id)
      .flatMap {
        case Some(board) if isUsernameContainedInBoard(username, board) =>
          if (!isUsernameContainedInBoard(deletedMemberUsername, board)) Future.successful(Left(BadRequestException("User does not have access to this board")))
          else
            deleteOneMemberFromBoard(id, deletedMemberUsername)
              .map {
                _ => Right()
              }
        case None => Future.successful(Left(NotFoundException("Board not found")))
        case _ => Future.successful(Left(ForbiddenException("You don't have access to this board")))
      }
  }
}